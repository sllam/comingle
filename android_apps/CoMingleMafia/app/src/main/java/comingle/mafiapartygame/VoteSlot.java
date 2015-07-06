package comingle.mafiapartygame;

import java.util.ArrayList;

/**
 * Created by AliElgazar on 5/30/2015.
 */
public class VoteSlot {
    public int numberOfVotes=0; //for vote counting and display
    public String name=""; //display name of player
    public int location=-1; //actual location of player
    public int votedForLoc=-1; //who did this player vote for this round
    public ArrayList<VoteSlot> votees=new ArrayList<VoteSlot>(); //the voteSlots who voted for this location -NOTE that this is for the secondary mafia display. IT IS NOT for the actual game itself.
    public int votedForLoc2=-1; //IF this person is a mafia, then this value will be used to indicate that on the secondary mafia display, this person voted for loc2
    public VoteSlot(int NOV,String name, int location){
        numberOfVotes=NOV;
        this.name=name;
        this.location=location;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof VoteSlot)) return false;

        VoteSlot voteSlot = (VoteSlot) o;

        if (numberOfVotes != voteSlot.numberOfVotes) return false;
        if (location != voteSlot.location) return false;
        return !(name != null ? !name.equals(voteSlot.name) : voteSlot.name != null);

    }

    @Override
    public int hashCode() {
        int result = numberOfVotes;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + location;
        return result;
    }
}
