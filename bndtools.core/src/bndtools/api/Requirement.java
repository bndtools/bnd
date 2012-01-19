package bndtools.api;

public class Requirement {
    private final String name;
    private final String filter;

    public Requirement(String name, String filter) {
        this.name = name;
        this.filter = filter;
    }

    public String getName() {
        return name;
    }

    public String getFilter() {
        return filter;
    }

    @Override
    public String toString() {
        return "Requirement [name=" + name + ", filter=" + filter + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((filter == null) ? 0 : filter.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Requirement other = (Requirement) obj;
        if (filter == null) {
            if (other.filter != null)
                return false;
        } else if (!filter.equals(other.filter))
            return false;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        return true;
    }


}
