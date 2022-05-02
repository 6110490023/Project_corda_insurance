"use strict";

angular.module('demoAppModule').controller('CreateClaimModalCtrl', function($http, $uibModalInstance, $uibModal, apiBaseURL, peers) {
    const createClaimModal = this;

    createClaimModal.peers = peers;
    createClaimModal.form = {};
    createClaimModal.formError = false;

    /** Validate and create claim. */
    createClaimModal.create = () => {
        if (invalidFormInput()) {
            createClaimModal.formError = true;
        } else {
            createClaimModal.formError = false;
            const HN = createClaimModal.form.HN
            const InsID = createClaimModal.form.InsID
            const amount = createClaimModal.form.amount;
            const party = createClaimModal.form.counterparty;
            
            $uibModalInstance.close();
            // We define the IOU creation endpoint.
            const createClaimEndpoint = apiBaseURL +
                `createClaim?HN=${HN}&InsID=${InsID}&insuranceName=${party}&amount=${amount}`;
                //apiBaseURL +
                //`issue-iou?amount=${amount}&currency=${currency}&party=${party}`;

            // We hit the endpoint to create the Claim and handle success/failure responses.
            
            
            $http.put(createClaimEndpoint).then(
                () => {consol.log("55555");},
                // () => {console.log("66666");},
                (result) => createIOUModal.displayMessage(result),
                (result) => createIOUModal.displayMessage(result)
    
            );
            
        }
    };

    /** Displays the success/failure response from attempting to create an IOU. */
    createClaimModal.displayMessage = (message) => {
        const createClaimMsgModal = $uibModal.open({
            templateUrl: 'createClaimMsgModal.html',
            controller: 'CreateClaimMsgModalCtrl',
            controllerAs: 'createClaimMsgModal',
            resolve: {
                message: () => message
            }
        });

        // No behaviour on close / dismiss.
        createClaimMsgModal.result.then(() => {}, () => {});
    };

    /** Closes the IOU creation modal. */
    createClaimModal.cancel = () => $uibModalInstance.dismiss();

    // Validates the IOU.
    function invalidFormInput() {
        return (createClaimModal.form.HN == null) || (createClaimModal.form.InsID==null) || isNaN(createClaimModal.form.amount) || (createClaimModal.form.counterparty === undefined);
    }
});

// Controller for the success/fail modal.
angular.module('demoAppModule').controller('CreateClaimMsgModalCtrl', function($uibModalInstance, message) {
    const createClaimMsgModal = this;
    createClaimMsgModal.message = message.data;
});