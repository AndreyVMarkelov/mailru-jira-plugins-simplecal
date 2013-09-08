package ru.andreymarkelov.atlas.plugins.simplecal;

public class ProjRole {
    private String project;
    private String role;

    public ProjRole(String project, String role) {
        this.project = project;
        this.role = role;
    }

    public String getProject() {
        return project;
    }

    public String getRole() {
        return role;
    }

    @Override
    public String toString() {
        return "ProjRoles[project=" + project + ", role=" + role + "]";
    }
}
