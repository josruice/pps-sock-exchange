n=100
t=100
p=g2

all:
	java exchange.sim.Simulator -p g1 g2 g3 g4 g5 g6 -t 100 -n 100 --silent

compile:
	javac exchange/sim/*.java

clean:
	rm exchange/*/*.class

verbose:
	java exchange.sim.Simulator -p ${p} ${p} -n ${n} -t ${t} -v
