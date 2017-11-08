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
    private final int K = 10;
    private HashMap<Integer, HashMap<Integer,Double>> THRESHOLD;
    private int threshold;
    private int offeringS1, offeringS2, id;
    private double offeringS1DistanceToCentroid, offeringS2DistanceToCentroid;

    private Sock[] socks;

    private int currentTurn;
    private int totalTurns;
    private int numPlayers;
    private int numSocks;
    private boolean shouldRecomputePairing;

    private AbstractEKmeans eKmeans;
    private Sock[] centroids;
    private SockDistanceFunction sockDistanceFunction;
    private SockCenterFunction sockCenterFunction;

    private PriorityQueue<SockPair> rankedPairs;
    private HashMap<Sock, Integer> offerSocks;
    private List<Offer> lastOffers = null;

    @Override
    public void init(int id, int n, int p, int t, List<Sock> socks) {
        this.id = id;
        this.totalTurns = t;
        this.currentTurn = t;
        this.numPlayers = p;
        this.numSocks = n*2;
        this.socks = (Sock[]) socks.toArray(new Sock[2 * n]);
        this.shouldRecomputePairing = false;
        this.threshold = 10;

        this.rankedPairs = new PriorityQueue<SockPair>();
        this.offerSocks = new HashMap<Sock, Integer>();
        this.THRESHOLD = createThreshold();

        System.out.println("Initial embarrassment for player " + id+ ": " + getEmbarrasment());
        pairAlgo();
    }

    private HashMap< Integer, HashMap<Integer,Double>> createThreshold(){
      HashMap<Integer, HashMap<Integer,Double>> result = new HashMap< Integer, HashMap<Integer,Double>>();

      HashMap<Integer,Double> n_10 = new HashMap<Integer,Double>();
      n_10.put(0,14.45683229);
      n_10.put(5,27.04070882);
      n_10.put(10,31.16629779);
      n_10.put(15,34.82567729);
      n_10.put(20,39.43039952);
      n_10.put(25,45.96228755);
      n_10.put(30,50.28188857);
      n_10.put(35,52.43379674);
      n_10.put(40,55.0781046);
      n_10.put(45,59.20605407);
      n_10.put(50,61.80862098);
      n_10.put(55,65.10040929);
      n_10.put(60,71.70616603);
      n_10.put(65,76.55716818);
      n_10.put(70,80.15536644);
      n_10.put(75,88.67706461);
      n_10.put(80,95.31330462);
      n_10.put(85,99.56778715);
      n_10.put(90,111.0624375);
      n_10.put(95,132.9023253);
      n_10.put(100,165.5173707);


      HashMap<Integer,Double> n_100 = new HashMap<Integer,Double>();
      n_100.put(0,1.414213562);
      n_100.put(5,11.35117446);
      n_100.put(10,14.45683229);
      n_100.put(15,16.99558441);
      n_100.put(20,18.83613249);
      n_100.put(25,20.53045432);
      n_100.put(30,22.12237714);
      n_100.put(35,24.0208243);
      n_100.put(40,25.63201124);
      n_100.put(45,27.12285462);
      n_100.put(50,28.97411483);
      n_100.put(55,30.39406074);
      n_100.put(60,32.54842459);
      n_100.put(65,34.27460854);
      n_100.put(70,36.28631663);
      n_100.put(75,38.73949498);
      n_100.put(80,41.01219331);
      n_100.put(85,44.17915406);
      n_100.put(90,47.75457836);
      n_100.put(95,53.57424692);
      n_100.put(100,96.64884893);




      HashMap<Integer,Double> n_1000 = new HashMap<Integer,Double>();
      n_1000.put(0,1.0);
      n_1000.put(5,5.385164807);
      n_1000.put(10,7.063961031);
      n_1000.put(15,8.062257748);
      n_1000.put(20,8.921769701);
      n_1000.put(25,9.848857802);
      n_1000.put(30,10.77032961);
      n_1000.put(35,11.79617614);
      n_1000.put(40,12.68857754);
      n_1000.put(45,13.49443368);
      n_1000.put(50,14.3874526);
      n_1000.put(55,15.06651917);
      n_1000.put(60,15.8113883);
      n_1000.put(65,16.74959622);
      n_1000.put(70,17.72004515);
      n_1000.put(75,18.6747591);
      n_1000.put(80,20.05492211);
      n_1000.put(85,21.35993629);
      n_1000.put(90,23.18187508);
      n_1000.put(95,25.72824175);
      n_1000.put(100,37.33630941);


      result.put(10, n_10);
      result.put(100, n_100);
      result.put(1000, n_1000);

      return result;
    }

    private double getEmbarrasment() {
        return getEmbarrasment(this.socks);
    }

    private double getEmbarrasment(Sock[] socks) {
        double result = 0;
        for (int i = 0; i < socks.length; i += 2){
            result += socks[i].distance(socks[i+1]);
        }
        return result;
    }

    public Sock[] pairAlgo(Sock[] socks) {
        Sock[] result;
        if(this.numSocks < 300){
            result = pairBlossom(socks);
        } else {
            result  = pairGreedily(socks);
        }
        return result;
    }

    public void pairAlgo() {
        if(this.numSocks < 300){
            this.socks = pairBlossom(this.socks, true);
        } else {
            this.socks = pairGreedily(this.socks, true);
        }
    }

    public void pairBlossom() {
        this.socks = pairBlossom(this.socks, true);
    }

    public Sock[] pairBlossom(Sock[] socks) {
        return pairBlossom(socks, false);
    }

    public Sock[] pairBlossom(Sock[] socks, boolean updateRankedPairs) {
        int[] match = new Blossom(getCostMatrix(socks), true).maxWeightMatching();
        List<Sock> result = new ArrayList<Sock>();
        for (int i=0; i < match.length; i++) {
            if (match[i] < i) continue;
            result.add(socks[i]);
            result.add(socks[match[i]]);
            if (updateRankedPairs) {
                this.rankedPairs.add(new SockPair(socks[i], socks[match[i]]));
            }
        }
        if (updateRankedPairs) {
            this.threshold = 10;
            updateOfferSocks(this.threshold);
        }
        return (Sock[]) result.toArray(new Sock[socks.length]);
    }

    public void pairGreedily() {
        this.socks = pairGreedily(this.socks, true);
    }

    public Sock[] pairGreedily(Sock[] socks) {
        return pairGreedily(socks, false);
    }

    private Sock[] pairGreedily(Sock[] socks, boolean updateRankedPairs) {
        PriorityQueue<SockPair> queue = new PriorityQueue<SockPair>();
        for (int i = 0; i < socks.length ; i++){
            for (int j = 0; j < i; j++){
                queue.add(new SockPair(socks[i],socks[j]));
            }
        }


        HashSet<Sock> matched = new HashSet<Sock>();
        while(matched.size() < socks.length ){
            SockPair pair = queue.poll();
            if(pair != null) {
                if(!matched.contains(pair.s1) && !matched.contains(pair.s2)){
                    matched.add(pair.s1);
                    socks[matched.size()-1] = pair.s1;
                    matched.add(pair.s2);
                    socks[matched.size()-1] = pair.s2;
                    if (updateRankedPairs) {
                        this.rankedPairs.add(pair);
                    }
                }
            }
        }
        if (updateRankedPairs) {
            this.threshold = 10;
            updateOfferSocks(this.threshold);
        }
        return socks;
    }

    private void updateOfferSocks(int threshold){
        PriorityQueue<SockPair> cloneRank = new PriorityQueue(rankedPairs);
        HashMap<Sock, Integer> cloneOffer = new HashMap(this.offerSocks);

        this.offerSocks.clear();
        for(SockPair pair:rankedPairs){
            if ( pair.distance > this.THRESHOLD.get(this.numSocks/2).get(threshold)){
                if(!cloneOffer.containsKey(pair.s1)){
                    this.offerSocks.put(pair.s1,0);
                } else {
                    this.offerSocks.put(pair.s1,cloneOffer.get(pair.s1));
                }
                if(!cloneOffer.containsKey(pair.s2)){
                  this.offerSocks.put(pair.s2,0);
                } else {
                    this.offerSocks.put(pair.s2,cloneOffer.get(pair.s2));
                }
            }
        }
    }

    private float[][] getCostMatrix(Sock[] socks) {
        int numSocks = socks.length;
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

    private double getMaxReductionInPairDistance(Sock s) {
        double maxDistanceReduction = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < this.socks.length; i+=2) {
            if (i == offeringS1 || i == offeringS2) continue; // Skip offered pair.
            double pairDistance = this.socks[i].distance(this.socks[i+1]);
            double distanceToFirst = this.socks[i].distance(s);
            double distanceToSecond = this.socks[i+1].distance(s);
            double distanceReduction = pairDistance - Math.min(distanceToFirst, distanceToSecond);
            if (distanceReduction > maxDistanceReduction) {
                maxDistanceReduction = distanceReduction;
            }
        }
        return maxDistanceReduction;
    }


    @Override
    public Offer makeOffer(List<Request> lastRequests, List<Transaction> lastTransactions) {
        /*
			lastRequests.get(i)		-		Player i's request last round
			lastTransactions		-		All completed transactions last round.
		 */
        currentTurn--;
        if (this.shouldRecomputePairing) {
            rankedPairs.clear();
            pairAlgo();
            this.shouldRecomputePairing = false;
        }

        double sum = 0.0;
        for (double d : this.offerSocks.values()) {
            sum += d;
        }

        while (this.threshold < 30 && (sum > 2 * this.offerSocks.size() || this.offerSocks.size() == 0)){
            this.threshold += 5;
            updateOfferSocks(this.threshold);
            sum = 0.0;
            for (double d : this.offerSocks.values()) {
                sum += d;
            }
        }

        Set<Map.Entry<Sock,Integer>> entries = offerSocks.entrySet();
        List<Map.Entry<Sock,Integer>> sortedEntries = new ArrayList<>(entries);
        Collections.sort(sortedEntries, new Comparator<Map.Entry<Sock, Integer>>() {
            public int compare(Map.Entry<Sock,Integer> x, Map.Entry<Sock,Integer> y) {
                return x.getValue() - y.getValue();
            }
        });
        // Get a sock from the offering pile
        offeringS1 = Arrays.asList(socks).indexOf(sortedEntries.get(0).getKey());
        offeringS2 = Arrays.asList(socks).indexOf(sortedEntries.get(1).getKey());
        this.offerSocks.put(this.socks[offeringS1],  this.offerSocks.get(this.socks[offeringS1]) + 1 );
        this.offerSocks.put(this.socks[offeringS2],  this.offerSocks.get(this.socks[offeringS2]) + 1 );

        return new Offer(this.socks[offeringS1],this.socks[offeringS2]);
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
        try {
            lastOffers = offers;

            double maxDistanceReduction = getMaxReductionInPairDistance(this.socks[offeringS1]);
            int firstId = -1;
            int firstRank = -1;

            double secondMaxDistanceReduction = getMaxReductionInPairDistance(this.socks[offeringS2]);
            int secondId = -1;
            int secondRank = -1;

            Sock[] socksNoofferingS1 = this.socks.clone();
            Sock[] socksNoofferingS2 = this.socks.clone();
            Sock[] socksNoofferingS1NorofferingS2 = this.socks.clone();

            double currentEmbarrasment = getEmbarrasment();
            double avgEmbarrasment;
            double embarrasmentExchangingofferingS1ForS1;
            double embarrasmentExchangingofferingS2ForS1;
            double embarrasmentExchangingofferingS1ForS2;
            double embarrasmentExchangingofferingS2ForS2;
            double embarrasmentExchangingofferingS1AndofferingS2;
            double minPairEmbarrasment = currentEmbarrasment;

            double minSingleEmbarrasment = currentEmbarrasment;
            int singleId = -1;
            int singleRank = -1;

            double[][][] singleExchangeEmbarrasments = new double[offers.size()][2][2];
            boolean keepLooking = true;
            for (int i = 0; i < offers.size() && keepLooking; ++i) {
                if (i == id) continue; // Skip our own offer.
                for (int j = 1; j < 3 && keepLooking; ++j) {
                    Sock s1 = offers.get(i).getSock(j);
                    if (s1 == null) continue;

                    socksNoofferingS1[offeringS1] = s1;
                    if (singleExchangeEmbarrasments[i][j-1][0] == 0.0){
                        singleExchangeEmbarrasments[i][j-1][0] = getEmbarrasment(pairAlgo(socksNoofferingS1));
                    }
                    embarrasmentExchangingofferingS1ForS1 = singleExchangeEmbarrasments[i][j-1][0];
                    if (embarrasmentExchangingofferingS1ForS1 > currentEmbarrasment) continue;

                    socksNoofferingS2[offeringS2] = s1;
                    if (singleExchangeEmbarrasments[i][j-1][1] == 0.0){
                        singleExchangeEmbarrasments[i][j-1][1] = getEmbarrasment(pairAlgo(socksNoofferingS2));
                    }
                    embarrasmentExchangingofferingS2ForS1 = singleExchangeEmbarrasments[i][j-1][1];
                    if (embarrasmentExchangingofferingS2ForS1 > currentEmbarrasment) continue;

                    avgEmbarrasment = (embarrasmentExchangingofferingS1ForS1 + embarrasmentExchangingofferingS2ForS1)/2;
                    if (avgEmbarrasment < minSingleEmbarrasment) {
                        minSingleEmbarrasment = avgEmbarrasment;
                        singleId = i;
                        singleRank = j;
                        keepLooking = true;
                    }

                    socksNoofferingS1NorofferingS2[offeringS1] = s1;
                    for (int k = i; k < offers.size() && keepLooking; ++k) {
                        if (k == id) continue; // Skip our own offer.
                        for (int l = j+1; l < 3 && keepLooking; ++l) {
                            Sock s2 = offers.get(k).getSock(l);
                            if (s2 == null) continue;

                            socksNoofferingS1[offeringS1] = s2;
                            if (singleExchangeEmbarrasments[k][l-1][0] == 0.0)
                                singleExchangeEmbarrasments[k][l-1][0] = getEmbarrasment(pairAlgo(socksNoofferingS1));
                            embarrasmentExchangingofferingS1ForS2 = singleExchangeEmbarrasments[k][l-1][0];
                            if (embarrasmentExchangingofferingS1ForS2 > currentEmbarrasment) continue;

                            socksNoofferingS2[offeringS2] = s2;
                            if (singleExchangeEmbarrasments[k][l-1][1] == 0.0)
                                singleExchangeEmbarrasments[k][l-1][1] = getEmbarrasment(pairAlgo(socksNoofferingS2));
                            embarrasmentExchangingofferingS2ForS2 = singleExchangeEmbarrasments[k][l-1][1];
                            if (embarrasmentExchangingofferingS2ForS2 > currentEmbarrasment) continue;

                            socksNoofferingS1NorofferingS2[offeringS2] = s2;
                            embarrasmentExchangingofferingS1AndofferingS2 = getEmbarrasment(pairAlgo(socksNoofferingS1NorofferingS2));

                            if (embarrasmentExchangingofferingS1AndofferingS2 > currentEmbarrasment) continue;
                            avgEmbarrasment = (embarrasmentExchangingofferingS1ForS1 + embarrasmentExchangingofferingS1ForS2 +
                                    embarrasmentExchangingofferingS2ForS1 + embarrasmentExchangingofferingS2ForS1 +
                                    embarrasmentExchangingofferingS1AndofferingS2) / 5;
                            if (avgEmbarrasment < minPairEmbarrasment) {
                                minPairEmbarrasment = avgEmbarrasment;
                                firstId = i;
                                firstRank = j;
                                secondId = k;
                                secondRank = l;
                                keepLooking = true; // Use this assignment to improve efficiency.
                            }
                        }
                    }
                }
            }

            if (minSingleEmbarrasment < minPairEmbarrasment) {
                return new Request(singleId, singleRank, -1, -1);
            } else {
                return new Request(firstId, firstRank, secondId, secondRank);
            }
//            return new Request(singleId, singleRank, -1, -1);
        } catch (Exception e) {
            e.printStackTrace();
            return new Request(-1, -1, -1, -1);
        }
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
        this.shouldRecomputePairing = true;

        int rank;
        Sock newSock;
        if (transaction.getFirstID() == id) {
            rank = transaction.getFirstRank();
            newSock = transaction.getSecondSock();
        } else {
            rank = transaction.getSecondRank();
            newSock = transaction.getFirstSock();
        }
        if (rank == 1) {
          socks[offeringS1] = newSock;
        } else {
          socks[offeringS2] = newSock;
        }
    }

    @Override
    public List<Sock> getSocks() {
        pairAlgo();
        return Arrays.asList(socks);
    }
}
