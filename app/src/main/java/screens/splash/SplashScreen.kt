package com.pocketbudget.app.screens.splash
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import com.pocketbudget.app.navigation.Screen


@Composable
fun SplashScreen(
    navController: NavController
) {


    val scale = remember {
        Animatable(0f)
    }


    LaunchedEffect(true) {


        scale.animateTo(
            targetValue = 1f,
            animationSpec = tween(800)
        )


        delay(2000)


        navController.navigate(
            Screen.Login.route
        ){

            popUpTo(
                Screen.Splash.route
            ){
                inclusive = true
            }

        }

    }



    Box(

        modifier = Modifier

            .fillMaxSize()

            .background(

                Brush.verticalGradient(

                    listOf(

                        Color(0xFF1D4ED8),

                        Color(0xFF2563EB)

                    )

                )

            ),


        contentAlignment = Alignment.Center

    ){


        Column(

            horizontalAlignment = Alignment.CenterHorizontally,

            modifier = Modifier
                .scale(scale.value)

        ){


            Icon(

                imageVector = Icons.Default.AccountBalanceWallet,

                contentDescription = null,

                tint = Color.White,

                modifier = Modifier.size(90.dp)

            )


            Spacer(
                modifier = Modifier.height(20.dp)
            )


            Text(

                text = "PocketBudget",

                color = Color.White,

                fontSize = 34.sp

            )


            Spacer(
                modifier = Modifier.height(8.dp)
            )


            Text(

                text = "Tu dinero bajo control",

                color = Color.White.copy(
                    alpha = 0.8f
                ),

                fontSize = 16.sp

            )


        }


    }


}