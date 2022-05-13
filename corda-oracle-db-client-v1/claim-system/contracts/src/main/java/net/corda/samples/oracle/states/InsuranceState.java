package net.corda.samples.oracle.states;

import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.samples.oracle.contracts.InsuranceContract;
import net.corda.core.contracts.LinearState;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.serialization.ConstructorForDeserialization;
import org.jetbrains.annotations.NotNull;
import net.corda.samples.oracle.states.Claim;

import java.util.ArrayList;
import java.util.List;
@BelongsToContract(InsuranceContract.class)
public class InsuranceState implements LinearState {
    private final String hospitalNumber;
    private final String insuranceID;
    private final Party insurance; // who create paper insurance.
    private final Party hospital;
    private final UniqueIdentifier linearId;
    private List<Claim> claims;
    // ALL Corda State required parameter to indicate storing parties
    private List<AbstractParty> participants;

    public InsuranceState(String hospitalNumber, String insuranceID, Party insurance, Party hospital, UniqueIdentifier linearId) {
        this.hospitalNumber = hospitalNumber;
        this.insuranceID = insuranceID;
        this.insurance = insurance;
        this.hospital = hospital;
        this.linearId = linearId;
        this.claims = new ArrayList<Claim>();
        this.participants = new ArrayList<AbstractParty>();
        this.participants.add(hospital);
        this.participants.add(insurance);
    }
    @ConstructorForDeserialization
    public InsuranceState(String hospitalNumber, String insuranceID, Party insurance, Party hospital, List<Claim> claims ,UniqueIdentifier linearId) {
        this.hospitalNumber = hospitalNumber;
        this.insuranceID = insuranceID;
        this.insurance = insurance;
        this.hospital = hospital;
        this.linearId = linearId;
        this.claims = claims;
        this.participants = new ArrayList<AbstractParty>();
        this.participants.add(hospital);
        this.participants.add(insurance);
    }
    @NotNull
    @Override
    public List<AbstractParty> getParticipants() {
        return participants;
    }
    @NotNull
    @Override
    public UniqueIdentifier getLinearId() {
        return linearId;
    }

    public String getHospitalNumber() {
        return hospitalNumber;
    }

    public String getInsuranceID() {
        return insuranceID;
    }

    public Party getInsurance() {
        return insurance;
    }

    public Party getHospital() {
        return hospital;
    }

    public List<Claim> getClaims() {
        return claims;
    }
    public int  getCountClaims() {
        return this.claims.size();
    }
    public InsuranceState updateInsuranceState(Claim claim){
        List<Claim> newClaims = new ArrayList<>(this.claims);
        newClaims.add(claim);
        InsuranceState newInsurance =  new InsuranceState(this.hospitalNumber,this.insuranceID,this.insurance,this.hospital,newClaims,this.linearId);
        return newInsurance;
    }
}

