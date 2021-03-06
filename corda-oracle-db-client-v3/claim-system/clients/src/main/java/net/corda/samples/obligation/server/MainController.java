package net.corda.samples.obligation.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.corda.client.jackson.JacksonSupport;
import net.corda.core.contracts.*;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.node.NodeInfo;
import net.corda.core.transactions.SignedTransaction;
import net.corda.samples.oracle.flows.ClaimInsuranceFlow;
import net.corda.samples.oracle.flows.CreateInsuranceFlow;
import net.corda.samples.oracle.states.InsuranceState;
import net.corda.samples.oracle.states.Claim;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.stream.Collectors;
import java.util.stream.Stream;

//import static net.corda.finance.workflows.GetBalances.getCashBalances;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;

/**
 * Define your API endpoints here.
 */
@RestController
@RequestMapping("/api/") // The paths for HTTP requests are relative to this base path.
public class MainController {
    private static final Logger logger = LoggerFactory.getLogger(RestController.class);
    private final CordaRPCOps proxy;
    private final CordaX500Name me;

    public MainController(NodeRPCConnection rpc) {
        this.proxy = rpc.getProxy();
        this.me = proxy.nodeInfo().getLegalIdentities().get(0).getName();

    }

    /** Helpers for filtering the network map cache. */
    public String toDisplayString(X500Name name){
        return BCStyle.INSTANCE.toString(name);
    }

    private boolean isNotary(NodeInfo nodeInfo) {
        return !proxy.notaryIdentities()
                .stream().filter(el -> nodeInfo.isLegalIdentity(el))
                .collect(Collectors.toList()).isEmpty();
    }

    private boolean isMe(NodeInfo nodeInfo){
        return nodeInfo.getLegalIdentities().get(0).getName().equals(me);
    }

    private boolean isNetworkMap(NodeInfo nodeInfo){
        return nodeInfo.getLegalIdentities().get(0).getName().getOrganisation().equals("Network Map Service");
    }

    @Configuration
    class Plugin {
        @Bean
        public ObjectMapper registerModule() {
            return JacksonSupport.createNonRpcMapper();
        }
    }

    @GetMapping(value = "/status", produces = TEXT_PLAIN_VALUE)
    private String status() {
        return "200";
    }

    @GetMapping(value = "/servertime", produces = TEXT_PLAIN_VALUE)
    private String serverTime() {
        return (LocalDateTime.ofInstant(proxy.currentNodeTime(), ZoneId.of("UTC"))).toString();
    }

    @GetMapping(value = "/addresses", produces = TEXT_PLAIN_VALUE)
    private String addresses() {
        return proxy.nodeInfo().getAddresses().toString();
    }

    @GetMapping(value = "/identities", produces = TEXT_PLAIN_VALUE)
    private String identities() {
        return proxy.nodeInfo().getLegalIdentities().toString();
    }

    @GetMapping(value = "/platformversion", produces = TEXT_PLAIN_VALUE)
    private String platformVersion() {
        return Integer.toString(proxy.nodeInfo().getPlatformVersion());
    }

    @GetMapping(value = "/peers", produces = APPLICATION_JSON_VALUE)
    public HashMap<String, List<String>> getPeers() {
        HashMap<String, List<String>> myMap = new HashMap<>();

        // Find all nodes that are not notaries, ourself, or the network map.
        Stream<NodeInfo> filteredNodes = proxy.networkMapSnapshot().stream()
                .filter(el -> !isNotary(el) && !isMe(el) && !isNetworkMap(el));
        // Get their names as strings
        List<String> nodeNames = filteredNodes.map(el -> el.getLegalIdentities().get(0).getName().toString())
                .collect(Collectors.toList());

        myMap.put("peers", nodeNames);
        return myMap;
    }

    @GetMapping(value = "/notaries", produces = TEXT_PLAIN_VALUE)
    private String notaries() {
        return proxy.notaryIdentities().toString();
    }

    @GetMapping(value = "/flows", produces = TEXT_PLAIN_VALUE)
    private String flows() {
        return proxy.registeredFlows().toString();
    }

    @GetMapping(value = "/states", produces = TEXT_PLAIN_VALUE)
    private String states() {
        return proxy.vaultQuery(ContractState.class).getStates().toString();
    }

    @GetMapping(value = "/me",produces = APPLICATION_JSON_VALUE)
    private HashMap<String, String> whoami(){
        HashMap<String, String> myMap = new HashMap<>();
        myMap.put("me", me.toString());
        return myMap;
    }
    /**
     * Example request:
     * curl -X GET 'http://localhost:3000/api/createClaim?HN=002715&InsID=A0840672&insuranceName=InsuranceA&amount=1000.0'
     * HN=002715&InsID=A0840672&insurance=InsuranceA&amount=1000.0
     */
    @PutMapping(value ="createInsurance" , produces = TEXT_PLAIN_VALUE )
    public  ResponseEntity<String> createInsurance(@RequestParam(value = "HN") String HN,
                                             @RequestParam(value = "InsID") String InsID,
                                             @RequestParam(value = "insuranceName") String insuranceName)throws IllegalArgumentException {
        Party insurance = Optional.ofNullable(proxy.wellKnownPartyFromX500Name(CordaX500Name.parse(insuranceName)))
                    .orElseThrow(() -> new IllegalArgumentException("Unknown insurance name."));
        try {
            SignedTransaction result = proxy.startTrackedFlowDynamic(CreateInsuranceFlow.CreateInsuranceInitiator.class,
                                            HN,InsID,insurance).getReturnValue().get();
            System.out.println("Maincotroller.java 153 ");
            System.out.println("Transaction id" + result.getId());
            return ResponseEntity
                        .status(HttpStatus.CREATED)
                        .body("Transaction id"+ result.getId() +" committed to ledger. \n " + result.getTx().getOutput(0));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PutMapping(value ="claimInsurance" , produces = TEXT_PLAIN_VALUE )
    public  ResponseEntity<String> claimInsurance(@RequestParam(value = "InsID") String InsID,
                                                  @RequestParam(value = "amount") double amount,
                                                  @RequestParam(value = "claimDes") String claimDes,
                                                   @RequestParam(value = "claimID") String claimID)throws IllegalArgumentException {
        try {
            SignedTransaction result = proxy.startTrackedFlowDynamic(ClaimInsuranceFlow.ClaimInsuranceInitiator.class,
                   InsID,amount,claimDes,claimID).getReturnValue().get();
            System.out.println("Maincotroller.java 172 ");
            System.out.println("Transaction id" + result.getId());
            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body("Transaction id"+ result.getId() +" committed to ledger. \n " + result.getTx().getOutput(0));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    //getInsuranceState
    @GetMapping(value = "/insurances",produces = APPLICATION_JSON_VALUE)
    public List<StateAndRef<InsuranceState>> getInsurances() {
        // Filter by states type: IOU.
        return proxy.vaultQuery(InsuranceState.class).getStates();
    }

    @GetMapping(value = "/claims",produces = APPLICATION_JSON_VALUE)
    public List<Claim> getClaims(@RequestParam(value = "InsID") String InsID){
        // Filter by states type: IOU.
        List<StateAndRef<InsuranceState>> insuranceStateAndRefs = proxy.vaultQuery(InsuranceState.class).getStates();
        StateAndRef<InsuranceState> inputStateAndRef = insuranceStateAndRefs.stream().filter(insuranceStateAndRef -> {
            InsuranceState insuranceState = insuranceStateAndRef.getState().getData();
            return insuranceState.getInsuranceID().equals(InsID);
        }).findAny().orElseThrow(() -> new IllegalArgumentException("Policy Not Found"));
        InsuranceState input = inputStateAndRef.getState().getData();
        List<Claim> listClaim = input.getClaims();
        return listClaim;
    }

}
