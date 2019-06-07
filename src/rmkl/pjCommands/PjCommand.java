package rmkl.pjCommands;

public abstract class PjCommand {

    /** returns true if has finished, false if still needs to be updated */
    public abstract boolean update(float dt);
    public abstract short type();
}



