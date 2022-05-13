package net.corda.samples.oracle.contracts;

import net.corda.samples.oracle.states.InsuranceState;
import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.Contract;
import net.corda.core.transactions.LedgerTransaction;
import org.jetbrains.annotations.NotNull;
import static net.corda.core.contracts.ContractsDSL.requireThat; //Domain Specific Language

public class InsuranceContract implements Contract {
    public static final String ID = "net.corda.samples.oracle.contracts.InsuranceContract";
    @Override
    public void verify(@NotNull LedgerTransaction tx) throws IllegalArgumentException {
        final CommandData commandData = tx.getCommands().get(0).getValue();
        if (commandData instanceof InsuranceContract.Commands.InsuranceClaim){
            InsuranceState output = tx.outputsOfType(InsuranceState.class).get(0);
            requireThat(require -> {
                require.using("This transaction should only have one user state as output", tx.getOutputs().size() == 1);
                require.using("The output user state should have name goods", !output.getHospitalNumber().equals(""));
                require.using("The output user state should have name goods", !output.getInsuranceID().equals(""));
                return null;
            });
        }
        else if (commandData instanceof InsuranceContract.Commands.AddClaim){
            InsuranceState output = tx.outputsOfType(InsuranceState.class).get(0);
            InsuranceState input = tx.inputsOfType(InsuranceState.class).get(0);
            requireThat(require -> {
                require.using("This transaction should only have one user state as output", tx.getOutputs().size() == 1);
                require.using("This transaction should only have one user state as input", tx.getInputs().size() == 1);
                require.using("The output user state should have name goods", output.getHospitalNumber().equals(input.getHospitalNumber()));
                require.using("The output user state should have name goods", output.getInsuranceID().equals(input.getInsuranceID()));
                return null;
            });
        }
        else{
            //Unrecognized Command type
            throw new IllegalArgumentException("Incorrect type of CreateClaimContract Commands");
        }
    }
    public interface Commands extends CommandData {
        //In our hello-world app, We will have two commands.
        class InsuranceClaim implements InsuranceContract.Commands {
            private final String insuranceID;
            private final Integer count;

            public InsuranceClaim(String insuranceID,Integer count) {
                this.insuranceID = insuranceID;
                this.count = count;
            }
            public Integer getCount() {
                return count;
            }
            public String getInsuranceID(){ return  insuranceID;}
        }
        class AddClaim implements InsuranceContract.Commands {
            private final String insuranceID;
            private final Integer count;

            public AddClaim(String insuranceID,Integer count) {
                this.insuranceID = insuranceID;
                this.count = count;
            }
            public Integer getCount() {
                return count;
            }
            public String getInsuranceID(){ return  insuranceID;}
        }
    }
}
