package com.tutorial.contracts;
import com.tutorial.states.Claim;
import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.Contract;
import net.corda.core.transactions.LedgerTransaction;
import org.jetbrains.annotations.NotNull;

import static net.corda.core.contracts.ContractsDSL.requireThat; //Domain Specific Language

public class ClaimContract implements Contract {
    // This is used to identify our contract when building a transaction.
    public static final String ID = "com.tutorial.contracts.ClaimContract";

    @Override
    public void verify(@NotNull LedgerTransaction tx) throws IllegalArgumentException {

        //Extract the command from the transaction.
        final CommandData commandData = tx.getCommands().get(0).getValue();

        //Verify the transaction according to the intention of the transaction
        if (commandData instanceof ClaimContract.Commands.CreateClaim){
            Claim output = tx.outputsOfType(Claim.class).get(0);
            requireThat(require -> {
                require.using("This transaction should only have one user state as output", tx.getOutputs().size() == 1);
                require.using("The output user state should have name goods", !output.getCustomerName().equals(""));
                require.using("The output user state should have amount goods", output.getAmount() > 0.0);
                return null;
            });
        }
        else if (commandData instanceof ClaimContract.Commands.AcceptClaim ) {
            //Retrieve the output state of the transaction
            Claim input = tx.inputsOfType(Claim.class).get(0);
            Claim output = tx.outputsOfType(Claim.class).get(0);

            //Using Corda DSL function requireThat to replicate conditions-checks
            requireThat(require -> {
                require.using("This transaction should consume one states", tx.getInputStates().size() == 1);
                require.using("The Insurance input equals Insurance output", input.getInsurance().equals(output.getInsurance()));
                require.using("The  input hostpital equals hostpital output", input.getHospital().equals(output.getHospital()));
                require.using("The basket of apple has to weight more than 0", output.getCount() > 0);
                return null;
            });
        }
        else{
            //Unrecognized Command type
            throw new IllegalArgumentException("Incorrect type of CreateClaimContract Commands");
        }
    }

    // Used to indicate the transaction's intent.
    public interface Commands extends CommandData {
        //In our hello-world app, We will have two commands.
        class CreateClaim implements ClaimContract.Commands {}
        class AcceptClaim implements ClaimContract.Commands {}

    }
}
