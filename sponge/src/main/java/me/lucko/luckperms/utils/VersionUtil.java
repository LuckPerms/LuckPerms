package me.lucko.luckperms.utils;

import de.icongmbh.oss.maven.plugin.javassist.ClassTransformer;
import javassist.*;
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.annotation.Annotation;
import javassist.bytecode.annotation.StringMemberValue;
import me.lucko.luckperms.LPSpongePlugin;

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
        CtMethod hackedVersionMethod = CtNewMethod.make("public String getVersion() { return \"" + this.version + "\"; }", clazz);
        clazz.removeMethod(getVersionMethod);
        clazz.addMethod(hackedVersionMethod);
    }

    @Override
    public void configure(final Properties properties) {
        assert properties != null;
        version = properties.getProperty("version");
    }
}