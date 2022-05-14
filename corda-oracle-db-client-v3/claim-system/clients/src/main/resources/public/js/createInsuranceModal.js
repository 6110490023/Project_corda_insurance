"use strict";

angular.module('demoAppModule').controller('CreateInsuranceModalCtrl', function($http, $uibModalInstance, $uibModal, apiBaseURL, peers) {
    const createInsuranceModal = this;

    createInsuranceModal.peers = peers;
    createInsuranceModal.form = {};
    createInsuranceModal.formError = false;

    /** Validate and create claim. */
    createInsuranceModal.create = () => {
        if (invalidFormInput()) {
            createInsuranceModal.formError = true;
        } else {
            createInsuranceModal.formError = false;
            const HN = createInsuranceModal.form.HN
            const InsID = createInsuranceModal.form.InsID
            const party = createInsuranceModal.form.counterparty;
            
            $uibModalInstance.close();
            // We define the IOU creation endpoint.
            const createInsuranceEndpoint = apiBaseURL +
                `createInsurance?HN=${HN}&InsID=${InsID}&insuranceName=${party}`;
                //apiBaseURL +
                //`issue-iou?amount=${amount}&currency=${currency}&party=${party}`;

            // We hit the endpoint to create the Claim and handle success/failure responses.
            $http.put(createInsuranceEndpoint).then(()=>{
                // Update the list of IOUs.

                $http.get(apiBaseURL + "insurances").then((response) => demoApp.insurances =
                Object.keys(response.data).map((key) => response.data[key].state.data));},
                (response) => createInsuranceModal.displayMessage(response),
                (response) => createInsuranceModal.displayMessage(response)
            );
            
        }
    };

    /** Displays the success/failure response from attempting to create an IOU. */
    createInsuranceModal.displayMessage = (message) => {
        const createInsuranceMsgModal = $uibModal.open({
            templateUrl: 'createInsuranceMsgModal.html',
            controller: 'CreateInsuranceMsgModalCtrl',
            controllerAs: 'createInsuranceMsgModal',
            resolve: {
                message: () => message
            }
        });

        // No behaviour on close / dismiss.
        createInsuranceMsgModal.result.then(() => {}, () => {});
    };

    /** Closes the IOU creation modal. */
    createInsuranceModal.cancel = () => $uibModalInstance.dismiss();

    // Validates the IOU.
    function invalidFormInput() {
        return (createInsuranceModal.form.HN == null) || (createInsuranceModal.form.InsID==null) || (createInsuranceModal.form.counterparty === undefined);
    }
});

// Controller for the success/fail modal.
angular.module('demoAppModule').controller('CreateInsuranceMsgModalCtrl', function($uibModalInstance, message) {
    const createInsuranceMsgModal = this;
    createInsuranceMsgModal.message = message.data;
});