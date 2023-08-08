# In App Subscription (Android Jetpack Compose)
In-App Subscription: https://github.com/miraljaviya/InAppSubscriptionCompose

In this repository i'm going to show you how to integrate In-App Subscription of Google Play Billing version 6+ in 6 steps. I follow the officailly google docs, i'm not using any third-party library.

Pre-requisite

1. Google Play Console Account
2. Published App on Play Store
3. Tester Device with GMS

```
Setup the in-app purchase subscription product in Google Play Console account
i have already created mine which are 
Product ID: sub_premium
```

### Step 1: Add the Google Play Billing Library dependency<br>
```gradle
//Add the Google Play Billing Library dependency to your app's build.gradle file as shown:

dependencies {

    def billingVersion = "6.0.1"
    implementation "com.android.billingclient:billing-ktx:$billingVersion"
    
}
```

```xml
//And Open Manifest File and add this permission
<uses-permission android:name="com.android.vending.BILLING" />

```


### Step 2: Initialize a BillingClient with PurchasesUpdatedListener<br>

 //Initialize a BillingClient with PurchasesUpdatedListener
 
![image](https://github.com/miraljaviya/InAppSubscriptionCompose/assets/56391753/5b0ed257-e903-44ea-8149-afdba9757875)


### Step 3: Establish a connection to Google Play<br>

![image](https://github.com/miraljaviya/InAppSubscriptionCompose/assets/56391753/2e9c14a1-97dc-4c95-a5c5-120d5020ea81)

### Step 4: Show subscription plan and launch the flow <br>

```kotlin

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
```

### Step 5: Processing subscription / Verify Payment<br>

```kotlin

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

```

### Step 6: Check Subscription (Already subscribed or not) <br>

```kotlin
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

```
