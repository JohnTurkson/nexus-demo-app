package com.johnturkson.demo

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.Composable
import androidx.compose.getValue
import androidx.lifecycle.ViewModelProvider
import androidx.ui.core.Alignment
import androidx.ui.core.ContentScale
import androidx.ui.core.Modifier
import androidx.ui.core.setContent
import androidx.ui.foundation.Box
import androidx.ui.foundation.Image
import androidx.ui.foundation.Text
import androidx.ui.foundation.VerticalScroller
import androidx.ui.layout.*
import androidx.ui.livedata.observeAsState
import androidx.ui.material.Card
import androidx.ui.material.ListItem
import androidx.ui.material.MaterialTheme
import androidx.ui.res.vectorResource
import androidx.ui.unit.dp
import com.johnturkson.common.model.LinkinbioPost

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val viewModel = ViewModelProvider(this).get(MainViewModel::class.java).apply {
            getLinkinbioPosts()
            subscribeToLinkinbioPostsUpdates()
        }
        
        setContent {
            MaterialTheme {
                Column {
                    LinkinbioUserCard()
                    
                    VerticalScroller {
                        LinkinbioPostsTable(viewModel.linkinbioPostsModel)
                    }
                }
            }
        }
    }
}

@Composable
fun LinkinbioUserCard() {
    Card {
        ListItem(
            text = { Text(text = "Linkin.bio") },
            secondaryText = { Text(text = "@demo") },
            icon = {
                Box(modifier = Modifier.preferredSize(48.dp).aspectRatio(1f)) {
                    Image(asset = vectorResource(id = R.drawable.ic_launcher_background))
                }
            }
        )
    }
}

@Composable
fun LinkinbioPostsTable(
    liveLinkinbioPosts: NonNullMutableLiveData<List<LinkinbioPost>>,
    elementsPerRow: Int = 3
) {
    val linkinbioPosts by liveLinkinbioPosts.observeAsState(initial = mutableListOf())
    val rows = linkinbioPosts.dividedIntoRows(elementsPerRow)
    Table(columns = elementsPerRow) {
        rows.forEach { row ->
            tableRow {
                row.forEach { linkinbioPost ->
                    Stack {
                        Image(
                            asset = vectorResource(id = R.drawable.ic_launcher_background),
                            modifier = Modifier.padding(1.dp).aspectRatio(1f),
                            contentScale = ContentScale.Crop
                        )
                        Text(text = linkinbioPost.url, modifier = Modifier.gravity(Alignment.Center))
                    }
                }
            }
        }
    }
}

fun <T> List<T>.dividedIntoRows(elementsPerRow: Int): List<List<T>> {
    return this.asSequence()
        .mapIndexed { index, element -> Pair(index, element) }
        .groupBy { (index, _) -> index / elementsPerRow }
        .toList()
        .map { (_, row) -> row }
        .map { row -> row.map { (_, element) -> element }.toList() }
        .toList()
}

fun <T> List<T>.replace(index: Int, replacement: T): List<T> {
    return this.mapIndexed { i, e -> if (i == index) replacement else e }.toList()
}
