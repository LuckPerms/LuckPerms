/*
 * Copyright (c) 2016 Lucko (Luck) <luck@lucko.me>
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */

package me.lucko.luckperms.sponge.utils;

import de.icongmbh.oss.maven.plugin.javassist.ClassTransformer;
import javassist.*;
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.annotation.Annotation;
import javassist.bytecode.annotation.StringMemberValue;
import me.lucko.luckperms.sponge.LPSpongePlugin;

import java.util.Properties;

/**
 * Sets the version value of the plugin.
 * Most of the code in this class was taken from:
 * https://github.com/icon-Systemhaus-GmbH/javassist-maven-plugin/blob/master/README.textile
 */
public class VersionUtil extends ClassTransformer {
    private String version = null;

    @Override
    protected boolean shouldTransform(CtClass clazz) throws NotFoundException {
        CtClass pluginClass = ClassPool.getDefault().get(LPSpongePlugin.class.getName());
        return !clazz.equals(pluginClass) && clazz.subtypeOf(pluginClass);
    }

    @Override
    protected void applyTransformations(CtClass clazz) throws Exception {
        AnnotationsAttribute attribute = (AnnotationsAttribute) clazz.getClassFile().getAttribute(AnnotationsAttribute.visibleTag);
        Annotation annotation = attribute.getAnnotation("org.spongepowered.api.plugin.Plugin");
        StringMemberValue versionValue = (StringMemberValue) annotation.getMemberValue("version");
        versionValue.setValue(version);
        attribute.setAnnotation(annotation);

        CtMethod getVersionMethod = clazz.getDeclaredMethod("getVersion");
        CtMethod hackedVersionMethod = CtNewMethod.make("public String getVersion() { return \"" + version + "\"; }", clazz);
        clazz.removeMethod(getVersionMethod);
        clazz.addMethod(hackedVersionMethod);
    }

    @Override
    public void configure(Properties properties) {
        assert properties != null;
        version = properties.getProperty("version");
    }
}