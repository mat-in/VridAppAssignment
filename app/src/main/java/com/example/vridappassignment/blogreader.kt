package com.example.vridappassignment

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

// Data Model
data class BlogPost(val id: Int, val title: Title, val link: String) {
    data class Title(val rendered: String)
}

// Retrofit API Service
interface BlogApiService {
    @GET("wp-json/wp/v2/posts")
    suspend fun getBlogs(
        @Query("per_page") perPage: Int,
        @Query("page") page: Int
    ): List<BlogPost>
}

// Retrofit Instance
object RetrofitInstance {
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://blog.vrid.in/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val api: BlogApiService = retrofit.create(BlogApiService::class.java)
}

// Fetch Blogs Function
fun fetchBlogs(page: Int, callback: (List<BlogPost>) -> Unit) {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val blogs = RetrofitInstance.api.getBlogs(perPage = 10, page = page)
            callback(blogs)
        } catch (e: Exception) {
            callback(emptyList())
        }
    }
}

// Blog List Screen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlogListScreen() {
    var blogs by remember { mutableStateOf<List<BlogPost>>(emptyList()) }
    var currentPage by remember { mutableStateOf(1) }
    val listState = rememberLazyListState()
    val context = LocalContext.current

    LaunchedEffect(currentPage) {
        fetchBlogs(currentPage) { fetchedBlogs ->
            blogs = blogs + fetchedBlogs
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Blogs") }) }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                items(blogs) { blog ->
                    BlogItem(blog) {
                        val intent = Intent(context, BlogDetailActivity::class.java)
                        intent.putExtra("BLOG_URL", blog.link)
                        context.startActivity(intent)
                    }
                }
            }

            LaunchedEffect(listState) {
                snapshotFlow { listState.layoutInfo.visibleItemsInfo }
                    .collect { visibleItems ->
                        if (visibleItems.isNotEmpty() && visibleItems.last().index == blogs.size - 1) {
                            currentPage++
                        }
                    }
            }
        }
    }
}

@Composable
fun BlogItem(blog: BlogPost, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Text(text = blog.title.rendered, modifier = Modifier.padding(16.dp))
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    BlogListScreen()
}


