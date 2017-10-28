package exchange.g2;

import java.util.*;

import exchange.sim.Offer;
import exchange.sim.Request;
import exchange.sim.Sock;
import exchange.sim.Transaction;

public class Player extends exchange.sim.Player {

    private final int K = 2;

    /*
        Inherited from exchange.sim.Player:
        Random random   -       Random number generator, if you need it

        Remark: you have to manually adjust the order of socks, to minimize the total embarrassment
                the score is calculated based on your returned list of getSocks(). Simulator will pair up socks 0-1, 2-3, 4-5, etc.
     */
    private int id1, id2, id;
    private double id1DistanceToCentroid, id2DistanceToCentroid;

    private Sock[] socks;

    private int currentTurn;
    private int totalTurns;
    private int numPlayers;
    private int numSocks;


    // Declare also the centroids so that they can be reused between iterations.
    private AbstractEKmeans eKmeans;
    private Sock[] centroids;
    private SockDistanceFunction sockDistanceFunction;
    private SockCenterFunction sockCenterFunction;

    @Override
    public void init(int id, int n, int p, int t, List<Sock> socks) {
        this.id = id;
        this.totalTurns = t;
        this.numPlayers = p;
        this.numSocks = n*2;
        this.socks = (Sock[]) socks.toArray(new Sock[2 * n]);

        // Initialize the centroids to random points (socks).
        this.centroids = new Sock[K];
        for (int i = 0; i < K; i++) {
            this.centroids[i] = new Sock(random.nextInt(256),
                                         random.nextInt(256),
                                         random.nextInt(256));
        }

        // Initialize the functions for distance and centroid centering.
        sockDistanceFunction = new SockDistanceFunction();
        sockCenterFunction = new SockCenterFunction();

        System.out.println("Initial embarrassment for player "+ id+ ": "+getEmbarrasment());
        pairSocksGreedily();
    }

    private void getSocksFarthestFromCentroid() {
        // Right now, we are only interested in the ids of the two socks with max distance from
        // their assigned centroids.
        computeClusters();

        double distance;
        double maxDistance = Double.NEGATIVE_INFINITY;
        int maxDistanceId = -1;
        double secondMaxDistance = Double.NEGATIVE_INFINITY;
        int secondMaxDistanceId = -1;

        int[] assignments = eKmeans.assignments;
        Sock assignedCentroid;
        for (int j = 0; j < this.socks.length; ++j) {
            assignedCentroid = centroids[assignments[j]];
            distance = assignedCentroid.distance(this.socks[j]);
            if (distance > maxDistance) {
                secondMaxDistance = maxDistance;
                secondMaxDistanceId = maxDistanceId;
                maxDistance = distance;
                maxDistanceId = j;
            } else if (distance > secondMaxDistance) {
                secondMaxDistance = distance;
                secondMaxDistanceId = j;
            }
        }

        this.id1 = maxDistanceId;
        this.id1DistanceToCentroid = maxDistance;
        this.id2 = secondMaxDistanceId;
        this.id2DistanceToCentroid = secondMaxDistance;
    }

    private void computeClusters() {
        this.eKmeans = new AbstractEKmeans<Sock, Sock>(
                this.centroids, this.socks, false,
                sockDistanceFunction, sockCenterFunction, null
        );
        eKmeans.run();
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
        pairSocksGreedily();
        getSocksFarthestFromCentroid();
        return new Offer(this.socks[id1], this.socks[id2]);
    }

    private double getMinDistanceToAnyCentroid(Sock s) {
        double minDistance = Double.POSITIVE_INFINITY;
        int minIndex = 0;
        for (int i = 0; i < this.centroids.length; ++i) {
            double distance = this.centroids[i].distance(s);
            if (distance < minDistance) {
                minDistance = distance;
                minIndex = i;
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
        double minDistance = this.id1DistanceToCentroid;
        int minDistanceOfferId = -1;
        int minDistanceOfferRank = -1;

        double secondMinDistance = this.id2DistanceToCentroid;
        int secondMinDistanceOfferId = -1;
        int secondMinDistanceOfferRank = -1;

        double distance;
        for (int i = 0; i < offers.size(); ++i) {
            if (i == id) continue; // Skip our own offer.
            for (int j = 1; j < 3; ++j) {
                Sock s = offers.get(i).getSock(j);
                if (s == null) continue;

                distance = getMinDistanceToAnyCentroid(s);
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
        if (rank == 1) socks[id1] = newSock;
        else socks[id2] = newSock;
    }

    @Override
    public List<Sock> getSocks() {
        return Arrays.asList(socks);
    }
}
