import os
import sys
import pdb
import subprocess
import statistics
from tqdm import tqdm

def save_float(x):
    try:
        return float(x)
    except ValueError:
        return None

def rotate_list(l, x):
    return l[-x:] + l[:-x]

num_pairs = 100
turnlimit = 50
timelimit = 100000  # in milliseconds
players = ['g1', 'g2', 'g3', 'g4', 'g5', 'g6']
repetition = 1

# Generating random seed for each run
primal_seed = 20171109
import random
random.seed(primal_seed)
seeds = [];
for i in range(repetition):
    seeds.append(random.randrange(2147483647))
print(seeds)


results = {}
for p in players:
    results[p] = {
        'initial_scores': [],
        'final_scores': []
    }

for run in tqdm(range(repetition)):
    # rotate the player list
    for k in tqdm(range(len(players))):
        p = open("tmp.log", "w")
        err = open("err.log", "w")
        players_to_run = rotate_list(players, k)
        # players_to_run = players
        subprocess.run(["java", "exchange.sim.Simulator", "--silent", "-s", str(seeds[run]), "-n", str(num_pairs), "-t", str(turnlimit), "-tl", str(timelimit), "-p"] + players_to_run, stdout = p, stderr = err)
        p.close()
        err.close()
        with open("tmp.log", "r") as log:
            t = log.readlines()[-len(players):]
            # print(t[(k+1)%len(players)], end='')
            for i in range(len(players)):
                parsed_log = [save_float(s) for s in t[i].split()]
                initial_score = parsed_log[-4]
                final_score = parsed_log[-1]
                if (t[i].find("illegal") != -1) or (t[i].find("timed") != -1):
                    final_score = -1
                results[players_to_run[i]]['initial_scores'].append(initial_score)
                results[players_to_run[i]]['final_scores'].append(final_score)
            # if (len(parsed) == 0):
            #     results.append(-1)
            # results.extend(parsed)
            log.close()

# TODO: Add your code here
for player, scores in results.items():
    initial_scores = scores['initial_scores']
    final_scores = scores['final_scores']
    score_reductions = [ini - fin for fin,ini in zip(final_scores, initial_scores)]
    print("\n" + player + " scores: %.2f" % statistics.mean(final_scores))
    print("  - [Final] Min, max: %d, %d" % (min(final_scores), max(final_scores)))
    print("  - [Final] Median: %.2f" % statistics.median(final_scores))
    print("  - [Final] Average: %.2f" % statistics.mean(final_scores))
    print("  - [Final] Standard deviation: %.2f" % statistics.pstdev(final_scores))
    print("  - [Reduc] Min, max: %d, %d" % (
        min(score_reductions), max(score_reductions)))
    print("  - [Reduc] Median: %.2f" % statistics.median(score_reductions))
    print("  - [Reduc] Average: %.2f" % statistics.mean(score_reductions))
    print("  - [Reduc] Standard deviation: %.2f" % statistics.pstdev(
        score_reductions))

print("\n\n### Summary (averaged final values):")
for player, scores in results.items():
    final_scores = scores['final_scores']
    print(player + " scores: %.2f" % statistics.mean(final_scores))
