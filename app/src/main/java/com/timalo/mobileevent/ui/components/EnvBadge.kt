package com.timalo.mobileevent.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.timalo.mobileevent.model.Environment
import com.timalo.mobileevent.ui.theme.AppColors

/** Badge coloré : orange en PRÉPROD, rouge en PROD. */
@Composable
fun EnvBadge(env: Environment, modifier: Modifier = Modifier) {
    val color = when (env) {
        Environment.PREPROD -> AppColors.Preprod
        Environment.PROD -> AppColors.Prod
    }
    Text(
        text = env.displayName,
        color = Color.White,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        modifier = modifier
            .background(color, RoundedCornerShape(6.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    )
}
