package net.corda.samples.oracle.flows;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.InitiatingFlow;
import net.corda.core.identity.Party;

@InitiatingFlow
public class QueryClaim extends FlowLogic<Integer>{
    private final Party oracle;
    private final String insuranceID;
    private final String commandOracle;
    public QueryClaim(Party oracle, String insuranceID,String commandOracle) {
        this.oracle = oracle;
        this.insuranceID = insuranceID;
        this.commandOracle =commandOracle;
    }
    @Suspendable
    @Override
    public Integer call() throws FlowException {
        String input = insuranceID+","+commandOracle;
        return initiateFlow(oracle).sendAndReceive(Integer.class, input).unwrap(it -> it);
    }
}
