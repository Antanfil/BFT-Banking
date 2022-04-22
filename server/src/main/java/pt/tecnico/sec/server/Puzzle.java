package pt.tecnico.sec.server;

import java.io.Serializable;
import java.util.Date;
public class Puzzle implements Serializable{
    int puzzleSolution = 0;
    Date timestamp = null;

    public Puzzle(int puzzleSolution, Date timestamp) {
        this.puzzleSolution = puzzleSolution;
        this.timestamp = timestamp;
    }

    public int getPuzzleSolution() {
        return puzzleSolution;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setPuzzleSolution(int puzzleSolution) {
        this.puzzleSolution = puzzleSolution;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    
    
}