package buildscript;

import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Copy;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;

import java.util.Arrays;
import java.util.Optional;



public abstract class CopyBuildResources extends Copy {
    @InputFiles
    public abstract Property<String> getResourcesPathString();

    public CopyBuildResources() {
        super.from(this.getResourcesPathString());
        super.into("build/resources");
    }
}
