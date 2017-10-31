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

    private int[][][] marketValue;
    private PriorityQueue<SockPair> rankedPairs;
    private List<Offer> lastOffers = null;

    @Override
    public void init(int id, int n, int p, int t, List<Sock> socks) {
        this.id = id;
        this.totalTurns = t;
        this.currentTurn = t;
        this.numPlayers = p;
        this.numSocks = n*2;
        this.socks = (Sock[]) socks.toArray(new Sock[2 * n]);

        this.marketValue = new int[8][8][8]; //Splitting into 8 equal sized rgb segments
        this.rankedPairs = new PriorityQueue<SockPair>();

        System.out.println("Initial embarrassment for player "+ id+ ": "+getEmbarrasment());
        pairBlossom();
    }

    private double getEmbarrasment() {
        double result = 0;
        for (int i = 0; i < this.numSocks; i += 2){
            result += this.socks[i].distance(this.socks[i+1]);
        }
        return result;
    }

    public void pairBlossom() {
        int[] match = new Blossom(getCostMatrix(), true).maxWeightMatching();
        List<Sock> result = new ArrayList<Sock>();
        for (int i=0; i<match.length; i++) {
            if (match[i] < i) continue;
            result.add(socks[i]);
            result.add(socks[match[i]]);
            this.rankedPairs.add(new SockPair(socks[i],socks[match[i]]));
        }
        socks = (Sock[]) result.toArray(new Sock[socks.length]);
    }

    private float[][] getCostMatrix() {
        float[][] matrix = new float[numSocks*(numSocks-1)/2][3];
        int idx = 0;
        for (int i = 0; i < socks.length; i++) {
            for (int j=i+1; j< socks.length; j++) {
                matrix[idx] = new float[]{i, j, (float)(-socks[i].distance(socks[j]))};
                idx ++;
            }
        }
        return matrix;
    }

    @Override
    public Offer makeOffer(List<Request> lastRequests, List<Transaction> lastTransactions) {
        /*
			lastRequests.get(i)		-		Player i's request last round
			lastTransactions		-		All completed transactions last round.
		 */
        //changing weights based on each request
        for (Request request : lastRequests) {
            if(request == null) continue;
            if(request.getFirstID() >= 0 && request.getFirstRank() >= 0) {
                Sock first = lastOffers.get(request.getFirstID()).getSock(request.getFirstRank());
                marketValue[first.R/32][first.G/32][first.B/32] += Math.pow(totalTurns-currentTurn,2);
            }
            if(request.getSecondID() >= 0 && request.getSecondRank() >= 0) {
                Sock second = lastOffers.get(request.getSecondID()).getSock(request.getSecondRank());
                marketValue[second.R/32][second.G/32][second.B/32] += Math.pow(totalTurns-currentTurn,2);
            }
        }

        currentTurn--;
        pairBlossom();

        //getting the worst paired socks
        SockPair maxMarketPair = rankedPairs.poll();
        int maxMarketValue = marketValue[maxMarketPair.s1.R/32][maxMarketPair.s1.G/32][maxMarketPair.s1.B/32] +
            marketValue[maxMarketPair.s1.R/32][maxMarketPair.s1.G/32][maxMarketPair.s1.B/32];

        for(int i=0; i<5; i++) {
            SockPair next = rankedPairs.poll();
            int nextMarketValue = marketValue[next.s1.R/32][next.s1.G/32][next.s1.B/32] + marketValue[next.s2.R/32][next.s2.G/32][next.s2.B/32];
            if (nextMarketValue > maxMarketValue) {
                maxMarketPair = next;
                maxMarketValue = nextMarketValue;
            }
        }
        id1 = getSocks().indexOf(maxMarketPair.s1);
        id2 = getSocks().indexOf(maxMarketPair.s2);
        rankedPairs.clear();

        return new Offer(maxMarketPair.s1,maxMarketPair.s2);
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
        //this is looking through each offer and changing the market value
        for( Offer offer : offers) {
            Sock first = offer.getFirst();
            Sock second = offer.getSecond();
            marketValue[first.R/32][first.G/32][first.B/32] -= Math.pow(totalTurns-currentTurn,2);
            marketValue[second.R/32][second.G/32][second.B/32] -= Math.pow(totalTurns-currentTurn,2);
        }
        lastOffers = offers;

        //write new method here

        return new Request(-1,-1,-1,-1);
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
        Sock one = socks[id1];
        Sock two = socks[id2];
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
