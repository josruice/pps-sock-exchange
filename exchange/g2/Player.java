package exchange.g2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Comparator;

import exchange.sim.Offer;
import exchange.sim.Request;
import exchange.sim.Sock;
import exchange.sim.Transaction;

class SockPair {
    double distance;
    Sock s1;
    Sock s2;

    public SockPair(Sock s1, Sock s2) {
        this.s1 = s1;
        this.s2 = s2;
        this.distance = s1.distance(s2);
    }
}

class SortByDistanceDesc implements Comparator<SockPair> {
    public int compare(SockPair a, SockPair b) {
        return (a.distance > b.distance)? -1 : 1;
    }
}

public class Player extends exchange.sim.Player {
    /*
        Inherited from exchange.sim.Player:
        Random random   -       Random number generator, if you need it

        Remark: you have to manually adjust the order of socks, to minimize the total embarrassment
                the score is calculated based on your returned list of getSocks(). Simulator will pair up socks 0-1, 2-3, 4-5, etc.
     */
    private int id1, id2, id;

    private Sock[] socks;
    private SockPair[] socksPairs;

    private int currentTurn;
    private int totalTurns;
    private int numPlayers;

    @Override
    public void init(int id, int n, int p, int t, List<Sock> socks) {
        this.id = id;
        this.totalTurns = t;
        this.numPlayers = p;
        this.socks = (Sock[]) socks.toArray(new Sock[2 * n]);
        this.socksPairs = new SockPair[n];

        pairSocksGreedily();
    }

    private void pairSocksGreedily() {
        for (int i = 0; i < this.socks.length-1; i+=2) {
            double minDistance = Double.POSITIVE_INFINITY;
            int minIndex = 0;
            for (int j = i+1; j < this.socks.length; ++j) {
                double distance = this.socks[i].distance(this.socks[j]);
                if (distance < minDistance) {
                    minDistance = distance;
                    minIndex = j;
                }
            }

            // Since we found the sock closest to i-th sock, let's put it in the i+1-th position so that it is not
            // considered again.
            Sock tmp = this.socks[i+1];
            this.socks[i+1] = this.socks[minIndex];
            this.socks[minIndex] = tmp;

            // Build the actual pair.
            this.socksPairs[(int) i/2] = new SockPair(this.socks[i], this.socks[i+1]);
        }
        sortPairsPerDistance();
    }

    private void sortPairsPerDistance() {
        Arrays.sort(this.socksPairs, new SortByDistanceDesc());
        for (int i = 0; i < this.socks.length-1; i+=2) {
            this.socks[i] = this.socksPairs[(int) i/2].s1;
            this.socks[i+1] = this.socksPairs[(int) i/2].s2;
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
