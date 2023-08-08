package com.mjcoding.inappsubsctioprion

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mjcoding.inappsubsctioprion.ui.theme.InAppSubscriptionTheme

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            InAppSubscriptionTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val chooseSubscription = remember {
                        ChooseSubscription(this)
                    }

                    LaunchedEffect(key1 = true) {
                        chooseSubscription.billingSetup()
                        chooseSubscription.hasSubscription()
                    }

                    val currentSubscription by chooseSubscription.subscriptions.collectAsState()

                    Column(
                        modifier = Modifier
                            .padding(vertical = 20.dp, horizontal = 10.dp)
                    ) {

                        Text(
                            text = "Subscriptions",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(horizontal = 20.dp)
                        )
                        ListItem(
                            headlineText = {
                                Text(
                                    text = "Github",
                                    style = MaterialTheme.typography.titleLarge
                                )
                            },
                            leadingContent = {
                                Text(text = if (currentSubscription.contains("github_sub")) "Purchased" else "Not Purchased")
                            }
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row {
                            TextButton(
                                onClick = {
                                    chooseSubscription.checkSubscriptionStatus("monthly")
                                }
                            ) {
                                Text(text = "Monthly plan")
                            }
                            TextButton(
                                onClick = {
                                    chooseSubscription.checkSubscriptionStatus("yearly")
                                }
                            ) {
                                Text(text = "Yearly plan")
                            }
                        }
                    }
                }
            }
        }
    }
}



@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    InAppSubscriptionTheme {

    }
}