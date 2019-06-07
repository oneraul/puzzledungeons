package rmkl;

import com.badlogic.gdx.utils.Array;

class PartyGroup {

    PjConnection leader;
    private final Array<PjConnection> players = new Array<>();
    int instancePort;
    String map = "OpenWorld";

    void addMember(PjConnection newMember) {

        // tell others about the new member
        Network.JoinParty p = new Network.JoinParty();
        p.member = newMember.name;
        for(PjConnection oldMember : players) {
            oldMember.sendTCP(p);
        }

        // add him to the list
        newMember.party = this;
        players.add(newMember);

        // tell him the complete list
        for(PjConnection c : players) {
            Network.JoinParty q = new Network.JoinParty();
            q.member = c.name;
            newMember.sendTCP(q);
        }

        // tell him which map is selected
        Network.MapSelected m = new Network.MapSelected();
        m.name = map;
        newMember.sendTCP(m);
    }

    void leaveParty(PjConnection member) {

        // tell everyone
        Network.LeaveParty p = new Network.LeaveParty();
        p.member = member.name;
        for(PjConnection c : players){
            c.sendTCP(p);
        }

        // remove player
        players.removeValue(member, true);
        member.party = null;

        // if group is empty -> cleanup
        if(players.size == 0) {
            leader = null;

        // set a new leader if they have left
        } else if(member == leader){
            leader = players.first();
            leader.sendTCP(new Network.PartyLeader());
        }
    }

    void setMap(Network.MapSelected p) {
        map = p.name;

        for(PjConnection c : players) {
            c.sendTCP(p);
        }
    }
}
