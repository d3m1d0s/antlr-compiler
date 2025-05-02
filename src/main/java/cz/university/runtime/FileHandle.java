package cz.university.runtime;

public class FileHandle {
    private final String name;
    private final String mode;

    public FileHandle(String name, String mode) {
        this.name = name;
        this.mode = mode;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "file(" + name + ")";
    }
}

