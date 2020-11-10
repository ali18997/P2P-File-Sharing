import java.util.Observable;

public class FlagObservable extends Observable {
    private Boolean flag;


    public FlagObservable(Boolean flag) {
        this.flag = flag;
    }

    public Boolean getFlag() {
        return flag;
    }


    public void setFlag(Boolean flag) {
        this.flag = flag;
        setChanged();
        notifyObservers(flag);
    }
}
