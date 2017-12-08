/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright (c) 2013-2020 Madz. All rights reserved.
 * 
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License"). You
 * may not use this file except in compliance with the License. You can
 * obtain a copy of the License at
 * https://raw.github.com/zhongdj/Lifecycle/master/License.txt
 * . See the License for the specific
 * language governing permissions and limitations under the License.
 * 
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 * 
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 * 
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license." If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above. However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */
package net.imadz.bcel;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.imadz.bcel.intercept.Interceptable;
import net.imadz.lifecycle.annotations.LifecycleMeta;
import net.imadz.lifecycle.annotations.ReactiveObject;
import net.imadz.lifecycle.annotations.Event;
import net.imadz.org.apache.bcel.classfile.AnnotationEntry;
import net.imadz.org.apache.bcel.classfile.Attribute;
import net.imadz.org.apache.bcel.classfile.ClassParser;
import net.imadz.org.apache.bcel.classfile.InnerClasses;
import net.imadz.org.apache.bcel.classfile.JavaClass;
import net.imadz.org.apache.bcel.classfile.Method;
import net.imadz.org.apache.bcel.generic.ClassGen;
import net.imadz.org.apache.bcel.generic.ObjectType;
import net.imadz.org.apache.bcel.generic.Type;

import static net.imadz.bcel.MethodInterceptor.POST_FIX;

public class BCELClassFileTransformer implements ClassFileTransformer {

    private static final Logger log = Logger.getLogger("Lifecycle Framework Byte Code Transformer");
    public static final String EVENT_ANNOTATION_TYPE = "L" + Event.class.getName().replaceAll("\\.", "/") + ";";
    public static final String LIFECYLEMETA_ANNOTATION_TYPE = "L" + LifecycleMeta.class.getName().replaceAll("\\.", "/") + ";";
    public static final String REACTIVE_ANNOTATION_TYPE = "L" + ReactiveObject.class.getName().replaceAll("\\.", "/") + ";";
    private String[] ignoredPackages = new String[] { "java.", "javax.", "sun." };

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer)
            throws IllegalClassFormatException {
        if ( shouldIgnore(className) ) {
            return classfileBuffer;
        }
        final String rawUrlPath = protectionDomain.getCodeSource().getLocation().getPath();
		final String location;
        if (rawUrlPath.contains("%20")) {
        	location = rawUrlPath.replaceAll("%20", " ");
        } else {
        	location = rawUrlPath;
        }
        ByteArrayInputStream bais = null; 
        try {
        	bais = new ByteArrayInputStream(classfileBuffer);
            final JavaClass jclas = new ClassParser(bais, className).parse();
            if ( !isTransformNeeded(jclas) ) {
                return classfileBuffer;
            }
            final ClassGen classGen = new ClassGen(jclas);
            int innerClassSeq = nextInnerClassSeqOf(classGen);
            for ( final Method method : jclas.getMethods() ) {
                if ( null == method.getAnnotationEntries() ) {
                    continue;
                }

                for ( final AnnotationEntry entry : method.getAnnotationEntries() ) {
                    if ( isTransformNeeded(entry) && notTransformYet(method, jclas.getMethods())) {
                        doTransform(classGen, innerClassSeq++, method, location);
                        break;
                    }
                }
            }
            return classGen.getJavaClass().getBytes();
        } catch (Throwable e) {
            log.log(Level.SEVERE, "Failed to transform class " + className, e);
            throw new IllegalClassFormatException();
        } finally {
        	if (null != bais) {
        		try {
					bais.close();
				} catch (IOException ignore) {}
            }
        }
    }

    private boolean notTransformYet(Method method, Method[] methods) {
        final String methodName = method.getName();
        final String finalMethodName = methodName + POST_FIX;
        for (final Method m : methods) {
            if (m.getName().equals(finalMethodName)) {
                return false;
            }
        }
        return true;
    }

    private boolean shouldIgnore(String className) {
        for ( int i = 0; i < ignoredPackages.length; i++ ) {
            if ( className.startsWith(ignoredPackages[i]) ) {
                return true;
            }
        }
        return false;
    }

    private boolean isTransformNeeded(AnnotationEntry entry) {
    	try {
    	String annotationTypeName = entry.getAnnotationType();
    	annotationTypeName = annotationTypeName.substring(1).replaceAll("/", ".");
    	annotationTypeName = annotationTypeName.substring(0, annotationTypeName.length() - 1);
    	return null != Class.forName(annotationTypeName).getAnnotation(Interceptable.class);
    	} catch (Exception ignored) {
    		//log.log(Level.OFF, "Failed to find class " + entry.getAnnotationType());
    		return false;
    	}
        //return EVENT_ANNOTATION_TYPE.equals(entry.getAnnotationType());
    }

    private int nextInnerClassSeqOf(final ClassGen cgen) {
        int innerClassSeq = 1;
        for ( final Attribute attribute : cgen.getAttributes() ) {
            if ( attribute instanceof InnerClasses ) {
                InnerClasses icAttr = (InnerClasses) attribute;
                innerClassSeq += icAttr.getInnerClasses().length;
            }
        }
        return innerClassSeq;
    }

    private boolean isTransformNeeded(final JavaClass jclas) {
        final AnnotationEntry[] annotationEntries = jclas.getAnnotationEntries();
        boolean foundLifecycleMeta = false;
        for ( final AnnotationEntry annotationEntry : annotationEntries ) {
        	if (isTransformNeeded(annotationEntry)) {
        		foundLifecycleMeta = true;
        	} else if ( LIFECYLEMETA_ANNOTATION_TYPE.equals(annotationEntry.getAnnotationType()) ) {
                foundLifecycleMeta = true;
            } else if ( REACTIVE_ANNOTATION_TYPE.equals(annotationEntry.getAnnotationType()) ) {
                foundLifecycleMeta = true;
            }
        }
        if (foundLifecycleMeta) {
            log.info("Transform Needed: " + jclas.getClassName());
        }
        return foundLifecycleMeta;
    }

    /**
     * 在main函数执行前，执行的函数
     * 
     * @param options
     * @param ins
     */
    public static void premain(String options, Instrumentation ins) {
        // 注册我自己的字节码转换器
        if ( log.isLoggable(Level.FINE) ) {
            log.fine("======================premain==========================");
        }
        ins.addTransformer(new BCELClassFileTransformer());
    }

    public static void agentmain(String args, Instrumentation inst) {
        // 注册我自己的字节码转换器
        if ( log.isLoggable(Level.FINE) ) {
            log.fine("======================agentmain==========================");
        }
        inst.addTransformer(new BCELClassFileTransformer());
    }

    private static void doTransform(ClassGen cgen, int innerClassSeq, Method interceptingMethod, String location) throws Throwable {
        JavaAnonymousInnerClass c = new JavaAnonymousInnerClass(cgen.getClassName(), interceptingMethod.getName(), interceptingMethod.getArgumentTypes(),
                innerClassSeq, Object.class.getName(), new Type[0], java.util.concurrent.Callable.class.getName(), new Type[] { new ObjectType(
                        Void.class.getName()) }, location);
        ClassGen doGenerate = c.doGenerate();
        doGenerate.getJavaClass().getBytes();
        MethodInterceptor.addWrapper(cgen, interceptingMethod, innerClassSeq);
        if ( "true".equals(System.getProperty("net.imadz.bcel.save.original")) ) {
            final String fileName;
            if ( '\\' == File.separatorChar ) {
                fileName = location + cgen.getClassName().replaceAll("\\.", "\\\\") + ".class";
            } else {
                fileName = location + cgen.getClassName().replaceAll("\\.", File.separator) + ".class";
            }
            FileOutputStream fos = new FileOutputStream(fileName);
            cgen.getJavaClass().dump(fos);
            fos.close();
        }
    }
}
