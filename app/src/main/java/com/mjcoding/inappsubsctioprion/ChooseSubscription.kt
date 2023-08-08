package com.mjcoding.inappsubsctioprion

import android.app.Activity
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.ConsumeResponseListener
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.google.firebase.crashlytics.buildtools.reloc.com.google.common.collect.ImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class ChooseSubscription(
    private val activity: Activity
) {
    private val _subscriptions = MutableStateFlow<List<String>>(emptyList())
    val subscriptions = _subscriptions.asStateFlow()

    private val purchaseUpdateListener = PurchasesUpdatedListener { result, purchases ->
        if (result.responseCode == BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                handlePurchase(purchase)
            }
        } else if (result.responseCode == BillingResponseCode.USER_CANCELED) {
            // User canceled the purchase
        } else {
            // Handle other error cases
        }
    }

    private var billingClient: BillingClient = BillingClient.newBuilder(activity)
        .setListener(purchaseUpdateListener)
        .enablePendingPurchases()
        .build()

    private fun handlePurchase(purchase: Purchase) {
        val consumeParams = ConsumeParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()

        val listener = ConsumeResponseListener { billingResult, s -> }

        billingClient.consumeAsync(consumeParams, listener)

        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            if (!purchase.isAcknowledged) {
                val acknowledgePurchaseParams = AcknowledgePurchaseParams
                    .newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()

                billingClient.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
                    if (billingResult.responseCode == BillingResponseCode.OK) {
                        _subscriptions.update {
                            val newList = it.toMutableList()
                            newList.addAll(purchase.products)
                            newList
                        }
                    }
                }
            }
        }
    }

    fun billingSetup() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingResponseCode.OK) {
                    hasSubscription()
                }
            }

            override fun onBillingServiceDisconnected() {
                // Handle billing service disconnection
            }
        })
    }

    fun hasSubscription() {
        val queryPurchaseParams = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        billingClient.queryPurchasesAsync(
            queryPurchaseParams
        ) { result, purchases ->
            when (result.responseCode) {
                BillingResponseCode.OK -> {
                    for (purchase in purchases) {
                        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                            // User has an active subscription
                            _subscriptions.update {
                                val newList = it.toMutableList()
                                newList.addAll(purchase.products)
                                newList
                            }
                            return@queryPurchasesAsync
                        }
                    }
                }

                BillingResponseCode.USER_CANCELED -> {
                    // User canceled the purchase
                }

                else -> {
                    // Handle other error cases
                }
            }

            // User does not have an active subscription

        }
    }

    fun checkSubscriptionStatus(
        subscriptionPlanId: String,
    ) {
        val queryPurchaseParams = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        billingClient.queryPurchasesAsync(
            queryPurchaseParams
        ) { result, purchases ->
            when (result.responseCode) {
                BillingResponseCode.OK -> {
                    for (purchase in purchases) {
                        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED && purchase.products.contains(
                                subscriptionPlanId
                            )
                        ) {
                            _subscriptions.update {
                                val newList = it.toMutableList()
                                newList.addAll(purchase.products)
                                newList
                            }
                            return@queryPurchasesAsync
                        }
                    }
                }

                BillingResponseCode.USER_CANCELED -> {
                    // User canceled the purchase
                }

                else -> {
                    // Handle other error cases
                }
            }
            // User does not have an active subscription
            querySubscriptionPlans(subscriptionPlanId)
        }
    }

    private fun querySubscriptionPlans(
        subscriptionPlanId: String,
    ) {
        val queryProductDetailsParams =
            QueryProductDetailsParams.newBuilder()
                .setProductList(
                    ImmutableList.of(
                        QueryProductDetailsParams.Product.newBuilder()
                            .setProductId("github_sub")
                            .setProductType(BillingClient.ProductType.SUBS)
                            .build(),
                    )
                )
                .build()

        billingClient.queryProductDetailsAsync(queryProductDetailsParams) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingResponseCode.OK) {
                var offerToken = ""
                val productDetails = productDetailsList.firstOrNull { productDetails ->
                    productDetails.subscriptionOfferDetails?.any {
                        if (it.basePlanId == subscriptionPlanId) {
                            offerToken = it.offerToken
                            true
                        } else {
                            false
                        }
                    } == true
                }
                productDetails?.let {
                    val productDetailsParamsList = listOf(
                        BillingFlowParams.ProductDetailsParams.newBuilder()
                            .setProductDetails(it)
                            .setOfferToken(offerToken)
                            .build()
                    )

                    val billingFlowParams = BillingFlowParams.newBuilder()
                        .setProductDetailsParamsList(productDetailsParamsList)
                        .build()

                    billingClient.launchBillingFlow(activity, billingFlowParams)
                }
            }
        }
    }
}
