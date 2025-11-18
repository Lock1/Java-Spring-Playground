package buildscript;

public enum GradleBuildProfiles {
    LOCAL("local"),
    PRODUCTION("prod");

    public final String stringRepresentation;

    private GradleBuildProfiles(String stringRepresentation) { this.stringRepresentation = stringRepresentation; }
}
