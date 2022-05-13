package net.corda.samples.oracle.flows;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.crypto.TransactionSignature;
import net.corda.core.flows.*;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.transactions.FilteredTransaction;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.samples.oracle.contracts.InsuranceContract;
import net.corda.samples.oracle.states.InsuranceState;
import net.corda.samples.oracle.states.Claim;

import net.corda.core.contracts.StateAndRef;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.QueryCriteria;

import java.util.List;
import java.util.UUID;
import java.util.Arrays;

public class ClaimInsuranceFlow {
    @InitiatingFlow
    @StartableByRPC
    public static class ClaimInsuranceInitiator  extends FlowLogic<SignedTransaction>{
        private String insuranceID; //เลขประกัน
        private double amount; // insurance Party
        private  String claimDescription;
        private  String claimID;

        public ClaimInsuranceInitiator(String insuranceID, double amount,String claimDescription,String claimID) {
            this.insuranceID = insuranceID;
            this.amount = amount;
            this.claimDescription = claimDescription;
            this.claimID = claimID;
        }
        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {
            //query state
            List<StateAndRef<InsuranceState>> insuranceStateAndRefs = getServiceHub().getVaultService()
                    .queryBy(InsuranceState.class).getStates();

            StateAndRef<InsuranceState> inputStateAndRef = insuranceStateAndRefs.stream().filter(insuranceStateAndRef -> {
                InsuranceState insuranceState = insuranceStateAndRef.getState().getData();
                return insuranceState.getInsuranceID().equals(this.insuranceID);
            }).findAny().orElseThrow(() -> new IllegalArgumentException("Policy Not Found"));

            //oracle เพื่อรับ count
            CordaX500Name oracleName = new CordaX500Name("Oracle", "Bangkok", "TH");
            Party oracle = getServiceHub().getNetworkMapCache().getNodeByLegalName(oracleName)
                    .getLegalIdentities().get(0);
            if (oracle == null) {
                throw new IllegalArgumentException("Requested oracle");
            }
            int count = subFlow(new QueryClaim(oracle,this.insuranceID));

            InsuranceState input = inputStateAndRef.getState().getData();
            Claim newClaim = new Claim(this.amount,count,this.claimID,this.claimDescription);

            InsuranceState output = input.updateInsuranceState(newClaim);

            TransactionBuilder txBuilder = new TransactionBuilder(inputStateAndRef.getState().getNotary())
                    .addInputState(inputStateAndRef)
                    .addOutputState(output, InsuranceContract.ID)
                    .addCommand(new InsuranceContract.Commands.AddClaim(output.getInsuranceID(),count),
                        Arrays.asList(oracle.getOwningKey(),getOurIdentity().getOwningKey(),input.getInsurance().getOwningKey()));
            // Verify that the transaction is valid.
            txBuilder.verify(getServiceHub());

            // Sign the transaction.
            final SignedTransaction partSignedTx = getServiceHub().signInitialTransaction(txBuilder);
            FilteredTransaction ftx = partSignedTx.buildFilteredTransaction(o -> {
                if (o instanceof Command && ((Command) o).getSigners().contains(oracle.getOwningKey())
                        && ((Command) o).getValue() instanceof InsuranceContract.Commands.AddClaim) {
                    return true;
                }
                return false;
            });

            TransactionSignature oracleSignature = subFlow(new SignPrime(oracle, ftx));
            SignedTransaction stx = partSignedTx.withAdditionalSignature(oracleSignature);

            // Send the state to the counterparty, and receive it back with their signature.
            FlowSession otherPartySession = initiateFlow(input.getInsurance());
            final SignedTransaction fullySignedTx = subFlow(new CollectSignaturesFlow(stx, Arrays.asList(otherPartySession)));
            // Notarise and record the transaction in both parties' vaults.
                return subFlow(new FinalityFlow(fullySignedTx, Arrays.asList(otherPartySession)));
        }
    }
    @InitiatedBy(ClaimInsuranceFlow.ClaimInsuranceInitiator.class)
    public static class ClaimInsuranceResponder extends FlowLogic<Void>{

        //private variable
        private FlowSession counterpartySession;

        public ClaimInsuranceResponder(FlowSession counterpartySession) {
            this.counterpartySession = counterpartySession;
        }

        @Override
        @Suspendable
        public Void call() throws FlowException {
            SignedTransaction signedTransaction = subFlow(new SignTransactionFlow(counterpartySession) {
                @Override
                @Suspendable
                protected void checkTransaction(SignedTransaction stx) throws FlowException {
                    System.out.println("Check Claim");
                    /*
                     * SignTransactionFlow will automatically verify the transaction and its signatures before signing it.
                     * However, just because a transaction is contractually valid doesn’t mean we necessarily want to sign.
                     * What if we don’t want to deal with the counterparty in question, or the value is too high,
                     * or we’re not happy with the transaction’s structure? checkTransaction
                     * allows us to define these additional checks. If any of these conditions are not met,
                     * we will not sign the transaction - even if the transaction and its signatures are contractually valid.
                     * ----------
                     * For this hello-world cordapp, we will not implement any additional checks.
                     * */
                }
            });

            //Stored the transaction into data base.
            subFlow(new ReceiveFinalityFlow(counterpartySession, signedTransaction.getId()));
            return null;
        }
    }
}
//String insuranceID, double amount,String claimDescription,String claimID
//flow start ClaimInsuranceInitiator insuranceID: A0840672, amount: 1000.0, claimDescription: "sick", claimID: A882036
//run vaultQuery contractStateType: net.corda.samples.oracle.states.InsuranceState