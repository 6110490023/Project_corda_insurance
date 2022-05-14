package net.corda.samples.oracle.services;

import net.corda.core.contracts.Command;
import net.corda.core.crypto.TransactionSignature;
import net.corda.core.node.ServiceHub;
import net.corda.core.node.services.CordaService;
import net.corda.core.serialization.SingletonSerializeAsToken;
import net.corda.core.transactions.ComponentVisibilityException;
import net.corda.core.transactions.FilteredTransaction;
import net.corda.core.transactions.FilteredTransactionVerificationException;
import net.corda.samples.oracle.contracts.ClaimContract;
import net.corda.samples.oracle.contracts.InsuranceContract;

import java.security.PublicKey;
import java.util.LinkedHashMap;
import java.util.Map;

import java.sql.*;



// We sub-class 'SingletonSerializeAsToken' to ensure that instances of this class are never serialised by Kryo.
// When a flows is check-pointed, the annotated @Suspendable methods and any object referenced from within those
// annotated methods are serialised onto the stack. Kryo, the reflection based serialisation framework we use, crawls
// the object graph and serialises anything it encounters, producing a graph of serialised objects.
// This can cause issues. For example, we do not want to serialise large objects on to the stack or objects which may
// reference databases or other external services (which cannot be serialised!). Therefore we mark certain objects with
// tokens. When Kryo encounters one of these tokens, it doesn't serialise the object. Instead, it creates a
// reference to the type of the object. When flows are de-serialised, the token is used to connect up the object
// reference to an instance which should already exist on the stack.
@CordaService
public class Oracle extends SingletonSerializeAsToken {

    static class MaxSizeHashMap<K, V> extends LinkedHashMap<K, V> {
        private final Integer maxSize;

        public MaxSizeHashMap() {
            this.maxSize = 1024;
        }

        public MaxSizeHashMap(int maxSize) {
            this.maxSize = maxSize;
        }

        @Override
        public boolean removeEldestEntry(Map.Entry<K, V> eldest) {
            return this.size() > maxSize;
        }
    }

    private final ServiceHub services;
    // Set the types of this to whatever query() takes and returns
    private final MaxSizeHashMap<Integer, Integer> cache = new MaxSizeHashMap<>();
    private final PublicKey myKey;

    public Oracle(ServiceHub services) {
        this.services = services;
        this.myKey = services.getMyInfo().getLegalIdentities().get(0).getOwningKey();
    }

    // The reason why prime numbers were chosen is because they are easy to reason about and reduce the mental load
    // for this tutorial application.
    // Clearly, most developers can generate a list of primes and all but the largest prime numbers can be verified
    // deterministically in reasonable time. As such, it would be possible to add a constraint in the
    // [PrimeContract.verify] function that checks the nth prime is indeed the specified number.

    public Integer query3(String insuranceID) {
        if (insuranceID == "") {
            throw new IllegalArgumentException("nameCustomer must be at least one Character.");
        }

        return getCountCliamFormDataBase(insuranceID);
    }

    // Signs over a transaction if the specified Nth prime for a particular N is correct.
    // This function takes a filtered transaction which is a partial Merkle tree. Any parts of the transaction which
    // the oracle doesn't need to see in order to verify the correctness of the nth prime have been removed. In this
    // case, all but the [PrimeContract.Create] commands have been removed. If the Nth prime is correct then the oracle
    // signs over the Merkle root (the hash) of the transaction.
    public TransactionSignature sign(FilteredTransaction ftx) throws FilteredTransactionVerificationException {
        // Check the partial Merkle tree is valid.
        ftx.verify();

        // Is it a Merkle tree we are willing to sign over?
        boolean isValidMerkleTree = ftx.checkWithFun(this::isCommandWithCorrectPrimeAndIAmSigner);
        try {
            /**
             * Function that checks if all of the commands that should be signed by the input public key are visible.
             * This functionality is required from Oracles to check that all of the commands they should sign are visible.
             */
            ftx.checkCommandVisibility(services.getMyInfo().getLegalIdentities().get(0).getOwningKey());
        } catch (ComponentVisibilityException e) {
            e.printStackTrace();
        }

        if (isValidMerkleTree) {
            return services.createSignature(ftx, myKey);
        } else {
            throw new IllegalArgumentException("Oracle signature requested over invalid transaction.");
        }
    }

    /**
     * Returns true if the component is an Create command that:
     * - States the correct prime
     * - Has the oracle listed as a signer
     */
    private boolean isCommandWithCorrectPrimeAndIAmSigner(Object elem) {
        if (elem instanceof Command && ((Command) elem).getValue() instanceof ClaimContract.Commands.CreateClaim) {
            ClaimContract.Commands.CreateClaim cmdData = (ClaimContract.Commands.CreateClaim) ((Command) elem).getValue();
            return (((Command) elem).getSigners().contains(myKey) && query3(cmdData.getInsuranceID()).equals(cmdData.getCount()));
        }
        else if (elem instanceof Command && ((Command) elem).getValue() instanceof InsuranceContract.Commands.InsuranceClaim) {
            InsuranceContract.Commands.InsuranceClaim cmdData = (InsuranceContract.Commands.InsuranceClaim) ((Command) elem).getValue();
            return (((Command) elem).getSigners().contains(myKey) && query3(cmdData.getInsuranceID()).equals(cmdData.getCount()));
        }
        else if (elem instanceof Command && ((Command) elem).getValue() instanceof InsuranceContract.Commands.AddClaim) {
            InsuranceContract.Commands.AddClaim cmdData = (InsuranceContract.Commands.AddClaim) ((Command) elem).getValue();
            return (((Command) elem).getSigners().contains(myKey) && query3(cmdData.getInsuranceID()).equals(cmdData.getCount()));
        }
        return false;
    }




    // generates countClaim

    private Integer getCountCliamFormDataBase(String insuranceID) {
        int count = -1;
        String InsID = "";

        try {
            Class.forName("com.mysql.jdbc.Driver");
            String name_db = "insurance_db";
            String name_table = "user_table";
            String url = "jdbc:mysql://localhost/"+name_db;
            Connection conn = null;
            conn  = DriverManager.getConnection(url,"root", "");
            System.out.println("Database is connected !");


            Statement stmt = conn.createStatement();
            String QUERY = "SELECT * from "+ name_table + " WHERE InsID = '"+insuranceID+"'";
            ResultSet rs = stmt.executeQuery(QUERY);
            while (rs.next()) {
                count = rs.getInt("count");
                InsID = rs.getString("InsID");
                System.out.println("InsID: " + rs.getString("InsID"));
                System.out.println("First: " + rs.getString("name"));
                System.out.println("Surname: " + rs.getString("surname"));
                System.out.println("count: " + count);
                System.out.println("limitCost: " + rs.getInt("limitCost"));
                System.out.println("cID: " + rs.getString("cid"));
            }
            rs.close();
            stmt.close();
            conn.close();
        }
        catch(Exception e) {
            System.out.print("Do not connect to DB - Error:"+e);
            e.printStackTrace();
        }

        return count;
    }
}
