"use strict";

angular.module('demoAppModule').controller('ClaimInsuranceModalCtrl', function($http, $uibModalInstance, $uibModal, apiBaseURL, peers) {
    const claimInsuranceModal = this;

    claimInsuranceModal.peers = peers;
    claimInsuranceModal.form = {};
    claimInsuranceModal.formError = false;

    /** Validate and create claim. */
    claimInsuranceModal.create = () => {
        if (invalidFormInput()) {
            claimInsuranceModal.formError = true;
        } else {
            claimInsuranceModal.formError = false;
            //flow start ClaimInsuranceInitiator insuranceID: A0840672, amount: 1000.0, claimDescription: "sick", claimID: A882036
            const InsID = claimInsuranceModal.form.InsID
            const amount = claimInsuranceModal.form.amount;
            const claimDes = claimInsuranceModal.form.claimDes;
            const claimID = claimInsuranceModal.form.claimID;

            $uibModalInstance.close();
            // We define the IOU creation endpoint.
            const claimInsuranceEndpoint = apiBaseURL +
                `claimInsurance?InsID=${InsID}&&amount=${amount}&&claimDes=${claimDes}&&claimID=${claimID}`;
                //apiBaseURL +
                //`issue-iou?amount=${amount}&currency=${currency}&party=${party}`;

            // We hit the endpoint to create the Claim and handle success/failure responses.
            $http.put(claimInsuranceEndpoint).then(
                // Update the list of IOUs.
                (response) => claimInsuranceModal.displayMessage(response),
                (response) => claimInsuranceModal.displayMessage(response)
            );
            
        }
    };

    /** Displays the success/failure response from attempting to create an IOU. */
    claimInsuranceModal.displayMessage = (message) => {
        const claimInsuranceMsgModal = $uibModal.open({
            templateUrl: 'claimInsuranceMsgModal.html',
            controller: 'ClaimInsuranceMsgModalCtrl',
            controllerAs: 'claimInsuranceMsgModal',
            resolve: {
                message: () => message
            }
        });

        // No behaviour on close / dismiss.
        claimInsuranceMsgModal.result.then(() => {}, () => {});
    };

    /** Closes the IOU creation modal. */
    claimInsuranceModal.cancel = () => $uibModalInstance.dismiss();

    // Validates the IOU.
    function invalidFormInput() {
        return (claimInsuranceModal.form.InsID==null) || isNaN(claimInsuranceModal.form.amount) || (claimInsuranceModal.form.claimDes == null)|| (claimInsuranceModal.form.claimID == null);
    }
});

// Controller for the success/fail modal.
angular.module('demoAppModule').controller('ClaimInsuranceMsgModalCtrl', function($uibModalInstance, message) {
    const claimInsuranceMsgModal = this;
    claimInsuranceMsgModal.message = message.data;
});