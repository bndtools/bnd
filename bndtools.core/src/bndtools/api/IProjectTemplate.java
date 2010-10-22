package bndtools.api;

public interface IProjectTemplate {
    void modifyInitialBndModel(IBndModel model);
    void modifyInitialBndProject(IBndProject project);
}
