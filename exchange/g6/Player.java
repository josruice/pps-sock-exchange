package exchange.g6;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
    private Sock median;
    private double max_dist1;//for stage 0 
    private double max_dist2;//for stage 0
    private int no_txn = 0; //count the number times with no txns 
    private int pair_offer = 0; //identify stages: pair_offer = 1 for stage1
    

    @Override
    public void init(int id, int n, int p, int t, List<Sock> socks) {
        this.id = id;
        this.socks = (Sock[]) socks.toArray(new Sock[2 * n]);
        this.median = new Sock(128,128,128);
        this.max_dist1 = -1;
        this.max_dist2 = -1;
    }

    @Override
    public Offer makeOffer(List<Request> lastRequests, List<Transaction> lastTransactions) {
        /*
			lastRequests.get(i)		-		Player i's request last round
			lastTransactions		-		All completed transactions last round.
		 */
            //increment no_txn
            this.no_txn += 1;
            //if stage 0
            if(this.no_txn <= 5){
                //Use greedy method to pair up socks
                pairSocks();
                //Find the median sock
                this.median = getMedianSock();

                //Get the farthest 2 socks
                this.max_dist1 = median.distance(socks[0]);
                this.max_dist2 = median.distance(socks[1]);
                int maxid1 = 0;
                int maxid2 = 1;
                if(this.max_dist1 < this.max_dist2){
                    double tmp = this.max_dist2;
                    this.max_dist2 = this.max_dist1;
                    this.max_dist1 = tmp;
                    maxid1 = 1;
                    maxid2 = 0;
                }

                for(int i = 0; i < socks.length; ++i){
                    if (median.distance(socks[i]) > this.max_dist1){
                        this.max_dist2 = this.max_dist1;
                        this.max_dist1 = median.distance(socks[i]);
                        maxid2 = maxid1;
                        maxid1 = i;
                    }
                    else if (median.distance(socks[i]) > this.max_dist2){
                        this.max_dist2 = median.distance(socks[i]);
                        maxid2 = i;
                    }
                }
                this.id1 = maxid1;
                this.id2 = maxid2;

                this.pair_offer = 0; //this is stage 0, so pair_offer == 0;

                //return the offer with two farthest socks
                return new Offer(socks[maxid1], socks[maxid2]);
        }
        else{//this is stage 1
            //get the pairs of socks backwards from the socks list.
            //when no_txn = 6,7,8,9 we have the last pair
            //when no_txn = 10,11,12,13,14 we have the second last pair.
            this.id1 = socks.length-(this.no_txn/5);
            this.id2 = socks.length-(this.no_txn/5)-1;
            if(this.id1 <= 0) {
                this.id1 = socks.length -1;
                this.id2 = socks.length -2;
                this.no_txn = 6;
            }
            this.pair_offer = 1; // this is stage 1
            return new Offer(socks[this.id1], socks[this.id2]);
        }
        //return null;
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

        int c = 0;
        List<Integer> availableOffers = new ArrayList<>();
        double min_dist1 = this.max_dist2;
        double min_dist2 = this.max_dist1;
        int min1 = -1;
        int min2 = -1;
        int rank1 = -1;
        int rank2 = -1;
        if(this.pair_offer == 0){ //if stage 0
            for (int i = 0; i < offers.size(); ++ i) {
                if (i == id) continue; //skip the current player's offer
                
                //look at other players' offer, and get socks that is closer to the median
                if(offers.get(i).getFirst().distance(this.median) < min_dist1){
                    min_dist2 = min_dist1;
                    min_dist1 = offers.get(i).getFirst().distance(this.median);
                    min2 = min1;
                    min1 = i;
                    rank2 = rank1;
                    rank1 = 1;
                    c +=1;
                }
                else if (offers.get(i).getFirst().distance(this.median) < min_dist2){
                    min_dist2 = offers.get(i).getFirst().distance(this.median);
                    min2 = i;
                    rank2 = 1;
                    c +=1;
                }
                if(offers.get(i).getSecond().distance(this.median) < min_dist1){
                    min_dist2 = min_dist1;
                    min_dist1 = offers.get(i).getSecond().distance(this.median);
                    min2 = min1;
                    min1 = i;
                    rank2 = rank1;
                    rank1 = 2;
                    c +=1;
                }
                else if (offers.get(i).getSecond().distance(this.median) < min_dist2){
                    min_dist2 = offers.get(i).getSecond().distance(this.median);
                    min2 = i;
                    rank2 = 2;
                    c +=1;
                }
            }
             }//no pair offer
        else{//if stage 1
            //get socks that can have smaller distance for the pair in the offer 
            for (int i = 0; i < offers.size(); ++ i) {
                if (i == id) continue;
                double dist = socks[id1].distance(socks[id2]); 
                 min_dist1 = dist;
                 min_dist2 =  dist;
                double dist_to_id1 = socks[id1].distance(offers.get(i).getFirst()); 
                double dist_to_id2 = socks[id2].distance(offers.get(i).getFirst());
                int close_id = id1;
                if(dist_to_id2 < dist_to_id1) close_id = id2;
                if(offers.get(i).getFirst().distance(socks[close_id]) < min_dist1){
                    min_dist2 = min_dist1;
                    min_dist1 = offers.get(i).getFirst().distance(socks[id1]);
                    min2 = min1;
                    min1 = i;
                    rank2 = rank1;
                    rank1 = 1;
                    c +=1;
                }
                else if (offers.get(i).getFirst().distance(socks[close_id]) < min_dist2){
                    min_dist2 = offers.get(i).getFirst().distance(socks[id1]);
                    min2 = i;
                    rank2 = 1;
                    c +=1;
                }
                dist_to_id1 = socks[id1].distance(offers.get(i).getSecond()); 
                dist_to_id2 = socks[id2].distance(offers.get(i).getSecond());
                close_id = id1;
                if(dist_to_id2 < dist_to_id1) close_id = id2;

                if(offers.get(i).getSecond().distance(this.median) < min_dist1){
                    min_dist2 = min_dist1;
                    min_dist1 = offers.get(i).getSecond().distance(this.median);
                    min2 = min1;
                    min1 = i;
                    rank2 = rank1;
                    rank1 = 2;
                    c +=1;
                }
                else if (offers.get(i).getSecond().distance(this.median) < min_dist2){
                    min_dist2 = offers.get(i).getSecond().distance(this.median);
                    min2 = i;
                    rank2 = 2;
                    c +=1;
                }
            }
            
        }
        //form the request
        if(c >=2 ){
            return new Request(min1, rank1 , min2, rank2);
        }
        else if(c == 1){
            if(min1 == -1){
                return new Request(min2, rank2, -1, -1);
            }
            else{
                return new Request(min1, rank1, -1, -1);
            }
        }
        else{
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


        //a txn happens, set no_txn to 0
        this.no_txn = 0;
        int rank;
        Sock newSock;
        Sock oldSock;
        if (transaction.getFirstID() == id) {
            rank = transaction.getFirstRank();
            newSock = transaction.getSecondSock();
            oldSock = transaction.getFirstSock();
        } else {
            oldSock = transaction.getSecondSock();
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

    public void pairSocks(){
        List<Sock>  next = new ArrayList<Sock>();
        for(int i = 0 ; i < socks.length; ++i){
            next.add(new Sock(socks[i].R, socks[i].G, socks[i].B));
        }
            
        int c = 0;
        ArrayList<Point> tmp_list = new ArrayList<Point>();
            for(int i = 0 ; i < next.size(); ++i){
                tmp_list.add(new Point(next.get(i).R, next.get(i).G, next.get(i).B));
            }

        while(c < socks.length-1){
            ClosestPair closep = new ClosestPair(tmp_list);
            closep.SolveByDivideAndConquer();
            Sock s1 = new Sock(closep.p1.x, closep.p1.y, closep.p1.z);
            // System.out.println("C is: " + c);
            socks[c] = s1;
            c+=1;
            Sock s2 = new Sock(closep.p2.x, closep.p2.y, closep.p2.z);
            socks[c] = s2;
            c+=1;
            ArrayList<Point> tmp_1 = new ArrayList<Point>();
            for(int i = 0 ; i < tmp_list.size(); ++i){
                if(tmp_list.get(i).x == closep.p1.x && tmp_list.get(i).y == closep.p1.y && tmp_list.get(i).z == closep.p1.z) continue;
                if(tmp_list.get(i).x == closep.p2.x && tmp_list.get(i).y == closep.p2.y && tmp_list.get(i).z == closep.p2.z) continue;
                tmp_1.add(new Point(tmp_list.get(i).x, tmp_list.get(i).y, tmp_list.get(i).z));
            }
            tmp_list = tmp_1;
            
        }
    }

    public Sock getMedianSock(){
        List<Integer> sock_R = new ArrayList<Integer>();
        List<Integer> sock_G = new ArrayList<Integer>();
        List<Integer> sock_B = new ArrayList<Integer>();
        for (Sock s : socks){
            sock_R.add(s.R);
            sock_G.add(s.G);
            sock_B.add(s.B);
        }

        Collections.sort(sock_R);
        Collections.sort(sock_G);
        Collections.sort(sock_B);

        int mid_R = sock_R.get(socks.length/2);
        int mid_G = sock_G.get(socks.length/2);
        int mid_B = sock_B.get(socks.length/2);

        return new Sock(mid_R, mid_G, mid_B);
    }

}
