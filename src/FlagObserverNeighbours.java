import java.util.Observable;
import java.util.Observer;

public class FlagObserverNeighbours implements Observer {
    private ActualMessageProcessor actualMessageProcessor;
    public FlagObserverNeighbours(ActualMessageProcessor actualMessageProcessor) {
        this.actualMessageProcessor = actualMessageProcessor;
    }

    public void update(Observable obj, Object arg) {
        actualMessageProcessor.updateNeighbours();
    }
}