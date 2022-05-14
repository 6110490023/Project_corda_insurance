"use strict";

// Define your backend here.
angular.module('demoAppModule', ['ui.bootstrap']).controller('DemoAppCtrl', function ($http, $location, $uibModal) {
    const demoApp = this;

    const apiBaseURL = "/api/";

    // Retrieves the identity of this and other nodes.
    let peers = [];
    $http.get(apiBaseURL + "me").then((response) => demoApp.thisNode = response.data.me);
    $http.get(apiBaseURL + "peers").then((response) => peers = response.data.peers);


    demoApp.openCreateInsuranceModal = () => {

        const createInsuranceModal =  $uibModal.open({
            templateUrl: 'createInsurance.html',
            controller: 'CreateInsuranceModalCtrl',
            controllerAs: 'createInsuranceModal',
            resolve: {
                apiBaseURL: () => apiBaseURL,
                peers: () => peers
            }
        });

        // Ignores the modal result events.
        createInsuranceModal.result.then(() => { }, () => { });
    };

    demoApp.openClaimInsuranceModal = () => {

            const claimInsuranceModal =  $uibModal.open({
                templateUrl: 'claimInsurance.html',
                controller: 'ClaimInsuranceModalCtrl',
                controllerAs: 'claimInsuranceModal',
                resolve: {
                    apiBaseURL: () => apiBaseURL,
                    peers: () => peers
                }
            });

            // Ignores the modal result events.
            claimInsuranceModal.result.then(() => { }, () => { });
    };
    /** Displays the IOU creation modal. */
    //    demoApp.openCreateIOUModal = () => {
    //        const createIOUModal = $uibModal.open({
    //            templateUrl: 'createIOUModal.html',
    //            controller: 'CreateIOUModalCtrl',
    //            controllerAs: 'createIOUModal',
    //            resolve: {
    //                apiBaseURL: () => apiBaseURL,
    //                peers: () => peers
    //            }
    //        });
    //
    //        // Ignores the modal result events.
    //        createIOUModal.result.then(() => {}, () => {});
    //    };

    /** Displays the cash issuance modal. */
    //    demoApp.openIssueCashModal = () => {
    //        const issueCashModal = $uibModal.open({
    //            templateUrl: 'issueCashModal.html',
    //            controller: 'IssueCashModalCtrl',
    //            controllerAs: 'issueCashModal',
    //            resolve: {
    //                apiBaseURL: () => apiBaseURL
    //            }
    //        });
    //
    //        issueCashModal.result.then(() => {}, () => {});
    //    };

    /** Displays the IOU transfer modal. */
    //    demoApp.openTransferModal = (id) => {
    //        const transferModal = $uibModal.open({
    //            templateUrl: 'transferModal.html',
    //            controller: 'TransferModalCtrl',
    //            controllerAs: 'transferModal',
    //            resolve: {
    //                apiBaseURL: () => apiBaseURL,
    //                peers: () => peers,
    //                id: () => id
    //            }
    //        });
    //
    //        transferModal.result.then(() => {}, () => {});
    //    };

    /** Displays the IOU settlement modal. */
    //    demoApp.openSettleModal = (id) => {
    //        const settleModal = $uibModal.open({
    //            templateUrl: 'settleModal.html',
    //            controller: 'SettleModalCtrl',
    //            controllerAs: 'settleModal',
    //            resolve: {
    //                apiBaseURL: () => apiBaseURL,
    //                id: () => id
    //            }
    //        });
    //
    //        settleModal.result.then(() => {}, () => {});
    //    };

    demoApp.fetchClaim = (InsID) => {
                // Update the list of IOUs.
                const claimEndpoint = apiBaseURL +`claims?InsID=${InsID}`;
                $http.get(claimEndpoint).then((response) => demoApp.claims = response.data);
                //$http.get(claimEndpoint).then((response) => console.log(response.data));
        }

    /** Refreshes the front-end. */
    demoApp.refresh = () => {
                // Update the list of IOUs.
                $http.get(apiBaseURL + "insurances").then((response) => demoApp.insurances =
                    Object.keys(response.data).map((key) => response.data[key].state.data));
                demoApp.claims = [];
        //        // Update the cash balances.
        //        $http.get(apiBaseURL + "cash-balances").then((response) => demoApp.cashBalances =
        //            response.data);
    }

    demoApp.refresh();
});

// Causes the webapp to ignore unhandled modal dismissals.
angular.module('demoAppModule').config(['$qProvider', function ($qProvider) {
    $qProvider.errorOnUnhandledRejections(false);
}]);