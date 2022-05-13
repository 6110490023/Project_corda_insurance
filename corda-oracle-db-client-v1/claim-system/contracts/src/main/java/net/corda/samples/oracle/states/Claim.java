package net.corda.samples.oracle.states;
import net.corda.core.serialization.CordaSerializable;
@CordaSerializable
public class Claim {
    private final double amount; // Treatment cost.
    private final int count;
    private final String claimID;
    private final String claimDescription;

    public Claim(double amount, int count, String claimID,String claimDescription) {
        this.amount = amount;
        this.count = count;
        this.claimID = claimID;
        this.claimDescription = claimDescription;
    }

    public double getAmount() {
        return amount;
    }

    public int getCount() {
        return count;
    }

    public String getClaimID() {
        return claimID;
    }
    public String getClaimDescription() {
        return claimDescription;
    }

}
