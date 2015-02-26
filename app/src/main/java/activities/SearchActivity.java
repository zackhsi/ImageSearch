package activities;

import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;

import com.etsy.android.grid.StaggeredGridView;
import com.zackhsi.imagesearch.R;

import java.util.ArrayList;

import adapters.ImageResultsAdapter;
import helpers.InfiniteScrollListener;
import models.GoogleImage;
import models.ImageResponse;
import models.ResultCursor;
import models.Settings;
import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;
import services.ImageService;


public class SearchActivity extends ActionBarActivity {
    private static final int SETTINGS_REQUEST_CODE = 0;
    private static final int RESULT_SIZE = 8;

    EditText etQuery;
    Button btnSearch;
    StaggeredGridView gvResults;
    ImageService api;

    ArrayList<GoogleImage> images;
    ImageResultsAdapter aImageResults;
    ResultCursor cursor;
    Settings settings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        initMemberVars();
        setupViews();
        setupApi();
        setupHandlers();
    }

    private void initMemberVars() {
        images = new ArrayList<>();
        aImageResults = new ImageResultsAdapter(this, images);
        cursor = new ResultCursor();
        settings = new Settings();
    }

    private void setupViews() {
        etQuery = (EditText) findViewById(R.id.etQuery);
        btnSearch = (Button) findViewById(R.id.btnSearch);
        gvResults = (StaggeredGridView) findViewById(R.id.gvResults);
        gvResults.setAdapter(aImageResults);
        gvResults.setOnScrollListener(new InfiniteScrollListener() {
            @Override
            public void onLoadMore(int page, int totalItemsCount) {
                loadMoreImages(page);
            }
        });
    }

    private void loadMoreImages(int page) {
        int nextPage = cursor.currentPageIndex + 1;
        if (nextPage  < cursor.pages.size()) {
            api.getImages("1.0", RESULT_SIZE,
                    settings.size,
                    settings.color,
                    settings.type,
                    settings.site,
                    nextPage * RESULT_SIZE,
                    etQuery.getText().toString(),
                    new Callback<ImageResponse>() {
                        @Override
                        public void success(ImageResponse imageResponse, Response response) {
                            if (imageResponse.responseData != null) {
                                aImageResults.addAll(imageResponse.responseData.results);
                            }
                            cursor = imageResponse.responseData.cursor;
                        }

                        @Override
                        public void failure(RetrofitError error) {
                            Log.i("DEBUG", "failure");
                        }
                    });
        }
    }

    private void setupApi() {
        RestAdapter restAdapter = new RestAdapter.Builder()
                .setEndpoint("https://ajax.googleapis.com/ajax/services/search")
                .build();

        api = restAdapter.create(ImageService.class);
    }

    private void setupHandlers() {
        btnSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                api.getImages("1.0", 8,
                        settings.size,
                        settings.color,
                        settings.type,
                        settings.site,
                        0,
                        etQuery.getText().toString(),
                        new Callback<ImageResponse>() {
                    @Override
                    public void success(ImageResponse imageResponse, Response response) {
                        aImageResults.clear();
                        aImageResults.addAll(imageResponse.responseData.results);
                        cursor = imageResponse.responseData.cursor;
                    }

                    @Override
                    public void failure(RetrofitError error) {
                        Log.i("DEBUG", "failure");
                    }
                });
            }
        });
        gvResults.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent i = new Intent(SearchActivity.this, ImageDetailActivity.class);
                i.putExtra("image", images.get(position));
                startActivityForResult(i, SETTINGS_REQUEST_CODE);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == SETTINGS_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                settings = (Settings) data.getSerializableExtra("settings");
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_search, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent i = new Intent(this, SettingsActivity.class);
            i.putExtra("settings", settings);
            startActivityForResult(i, SETTINGS_REQUEST_CODE);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
