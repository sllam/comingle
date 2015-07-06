package comingle.mafiapartygame;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

import comingle.comms.directory.NodeInfo;

/**
 * Created by AliElgazar on 5/30/2015.
 */
public class arrayAdapter extends android.widget.ArrayAdapter<VoteSlot> {
    private int voteId;
    private int nameId;
    private int arrayAdapterId;
    public ArrayList<VoteSlot> votes;
    private Activity context;
    public arrayAdapter(Activity act,int arrayAdapterId, int nameId,int voteId, ArrayList<VoteSlot> votes){
        super(act, arrayAdapterId,votes);
        this.arrayAdapterId=arrayAdapterId;
        this.voteId=voteId;
        this.nameId=nameId;
        this.votes=votes;
        this.context=act;
    }
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        if (v == null) {
            if(context==null){
                Log.wtf("null","null");
            }
            LayoutInflater vi = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = vi.inflate(arrayAdapterId, null);
        }
        VoteSlot selected_vote = this.votes.get(position);
        if(selected_vote != null) {
            TextView name = (TextView)v.findViewById(this.nameId);
            TextView vote = (TextView)v.findViewById(this.voteId);
            if(name != null) {
                name.setText(selected_vote.name);
            }

            if(vote != null) {
                vote.setText(String.format("%s", new Object[]{Integer.valueOf(selected_vote.numberOfVotes)}));
            }

        }

        return v;
    }
}
