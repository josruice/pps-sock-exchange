package exchange.g2;

import java.util.*;

import exchange.sim.Offer;
import exchange.sim.Request;
import exchange.sim.Sock;
import exchange.sim.Transaction;

public class Player extends exchange.sim.Player {
    /*
        Inherited from exchange.sim.Player:
        Random random   -       Random number generator, if you need it

        Remark: you have to manually adjust the order of socks, to minimize the total embarrassment
                the score is calculated based on your returned list of getSocks(). Simulator will pair up socks 0-1, 2-3, 4-5, etc.
     */
    private int id1, id2, id;

    private Sock[] socks;
    private int numSocks;
    private int currentTurn;
    private int totalTurns;
    private int numPlayers;


    @Override
    public void init(int id, int n, int p, int t, List<Sock> socks) {
        this.id = id;
        this.totalTurns = t;
        this.numPlayers = p;
        this.socks = (Sock[]) socks.toArray(new Sock[2 * n]);
        this.numSocks = n*2;

        System.out.println("Initial embarrassment for player "+ id+ ": "+getEmbarrasment());
        pairSocksGreedily();
    }

    private double getEmbarrasment() {
        double result = 0;
        for (int i = 0; i < this.numSocks; i += 2){
            result += this.socks[i].distance(this.socks[i+1]);
        }
        return result;
    }

    private void pairSocksGreedily() {
        PriorityQueue<SockPair> queue = new PriorityQueue<SockPair>();
        for (int i = 0; i < this.socks.length ; i++){
            for (int j = 0; j < i; j++){
                queue.add(new SockPair(this.socks[i],this.socks[j]));
            }
        }

        HashSet<Sock> matched = new HashSet<Sock>();
        while(matched.size() < this.numSocks ){
            SockPair pair = queue.poll();
            if(pair != null) {
                if(!matched.contains(pair.s1) && !matched.contains(pair.s2)){
                    matched.add(pair.s1);
                    this.socks[matched.size()-1] = pair.s1;
                    matched.add(pair.s2);
                    this.socks[matched.size()-1] = pair.s2;
                }
            }
        }
    }

    @Override
    public Offer makeOffer(List<Request> lastRequests, List<Transaction> lastTransactions) {
        /*
			lastRequests.get(i)		-		Player i's request last round
			lastTransactions		-		All completed transactions last round.
		 */

        // Offer pair with the longest distance.
        pairSocksGreedily();
        return new Offer(this.socks[0], this.socks[1]);
    }

    private double getMinDistanceToNonOfferedSocks(Sock s) {
        // Assumming socks 0 and 1 are the ones offered.
        double minDistance = Double.POSITIVE_INFINITY;
        for (int i = 2; i < this.socks.length; ++i) {
            double distance = this.socks[i].distance(s);
            if (distance < minDistance) {
                minDistance = distance;
            }
        }
        return minDistance;
    }

    @Override
    public Request requestExchange(List<Offer> offers) {
		/*
			offers.get(i)			-		Player i's offer
			For each offer:
			offer.getSock(rank = 1, 2)		-		get rank's offer
			offer.getFirst()				-		equivalent to offer.getSock(1)
			offer.getSecond()				-		equivalent to offer.getSock(2)

			Remark: For Request object, rank ranges between 1 and 2
		 */
        double firstOfferedSockDistance = getMinDistanceToNonOfferedSocks(this.socks[0]);
        double secondOfferedSockDistance = getMinDistanceToNonOfferedSocks(this.socks[1]);

		double minOfferedSocksDistance = Math.min(firstOfferedSockDistance, secondOfferedSockDistance);
		double maxOfferedSocksDistance = Math.max(firstOfferedSockDistance, secondOfferedSockDistance);

        double minDistance = minOfferedSocksDistance;
        int minDistanceOfferId = -1;
        int minDistanceOfferRank = -1;

        double secondMinDistance = maxOfferedSocksDistance;
        int secondMinDistanceOfferId = -1;
        int secondMinDistanceOfferRank = -1;

        double distance;
        for (int i = 0; i < offers.size(); ++i) {
            if (i == id) continue; // Skip our own offer.
            for (int j = 1; j < 3; ++j) {
                Sock s = offers.get(i).getSock(j);
                if (s == null) continue;

                distance = getMinDistanceToNonOfferedSocks(s);
                if (distance < minDistance) {
                    secondMinDistance = minDistance;
                    secondMinDistanceOfferId = minDistanceOfferId;
                    secondMinDistanceOfferRank = minDistanceOfferRank;

                    minDistance = distance;
                    minDistanceOfferId = i;
                    minDistanceOfferRank = j;
                } else if (distance < secondMinDistance) {
                    secondMinDistance = distance;
                    secondMinDistanceOfferId = i;
                    secondMinDistanceOfferRank = j;
                }
            }
        }

        return new Request(minDistanceOfferId, minDistanceOfferRank, secondMinDistanceOfferId, secondMinDistanceOfferRank);
    }

    @Override
    public void completeTransaction(Transaction transaction) {
        /*
            transaction.getFirstID()        -       first player ID of the transaction
            transaction.getSecondID()       -       Similar as above
            transaction.getFirstRank()      -       Rank of the socks for first player
            transaction.getSecondRank()     -       Similar as above
            transaction.getFirstSock()      -       Sock offered by the first player
            transaction.getSecondSock()     -       Similar as above

            Remark: rank ranges between 1 and 2
         */
        int rank;
        Sock newSock;
        if (transaction.getFirstID() == id) {
            rank = transaction.getFirstRank();
            newSock = transaction.getSecondSock();
        } else {
            rank = transaction.getSecondRank();
            newSock = transaction.getFirstSock();
        }
        if (rank == 1) socks[0] = newSock;
        else socks[1] = newSock;
    }

    @Override
    public List<Sock> getSocks() {
        return Arrays.asList(socks);
    }
}
