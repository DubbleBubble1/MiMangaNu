package ar.rulosoft.mimanganu;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.ActionBar;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Toast;

import com.fedorvlasov.lazylist.ImageLoader;
import com.melnykov.fab.FloatingActionButton;

import ar.rulosoft.mimanganu.componentes.ControlInfo;
import ar.rulosoft.mimanganu.componentes.Database;
import ar.rulosoft.mimanganu.componentes.Manga;
import ar.rulosoft.mimanganu.servers.ServerBase;

public class FragmentDetails extends Fragment {

    public static final String TITLE = "titulo_m";
    public static final String PATH = "path_m";

    private static final String TAG = "FragmentDetails";
    String title, path;
    int id;
    private ImageLoader imageLoader;
    private ControlInfo data;
    private SwipeRefreshLayout str;
    private ServerBase s;
    private Manga m;
    private FloatingActionButton button_add;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        title = getArguments().getString(TITLE);
        path = getArguments().getString(PATH);
        id = getArguments().getInt(FragmentMainMisMangas.SERVER_ID);
        return inflater.inflate(R.layout.activity_detalles, container, false);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public void onStart() {
        super.onStart();
        data = (ControlInfo) getView().findViewById(R.id.datos);
        str = (SwipeRefreshLayout) getView().findViewById(R.id.str);
        ActionBar mActBar = getActivity().getActionBar();
        if (mActBar != null) {
            mActBar.setDisplayHomeAsUpEnabled(true);
        }
        button_add = (FloatingActionButton) getView().findViewById(R.id.button_add);
        button_add.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                new AddMangaTask().execute(m);
                AnimatorSet set = new AnimatorSet();
                ObjectAnimator anim1 = ObjectAnimator.ofFloat(button_add, "alpha", 1.0f, 0.0f);
                anim1.setDuration(0);
                DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
                ObjectAnimator anim2 = ObjectAnimator.ofFloat(button_add, "y", displayMetrics.heightPixels);
                anim2.setDuration(500);
                set.playSequentially(anim2, anim1);
                set.start();
            }
        });
        if (getActivity() != null) {
            button_add.setColorNormal(((MainActivity) getActivity()).colors[1]);
            button_add.setColorPressed(((MainActivity) getActivity()).colors[3]);
            button_add.setColorRipple(((MainActivity) getActivity()).colors[0]);
            str.setColorSchemeColors(((MainActivity) getActivity()).colors[0], ((MainActivity) getActivity()).colors[1]);
            data.setColor(((MainActivity) getActivity()).darkTheme, ((MainActivity) getActivity()).colors[0]);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                Window window = getActivity().getWindow();
                window.setNavigationBarColor(((MainActivity) getActivity()).colors[0]);
                window.setStatusBarColor(((MainActivity) getActivity()).colors[4]);
            }
            ((MainActivity)getActivity()).getSupportActionBar().setTitle(getResources().getString(R.string.datosde) + " " + title);
        }
        button_add.attachToScrollView(data);
        m = new Manga(id, title, path, false);
        s = ServerBase.getServer(id);
        imageLoader = new ImageLoader(this.getActivity());
        str.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                new LoadDetailsTask().execute();
            }
        });
        str.post(new Runnable() {
            @Override
            public void run() {
                str.setRefreshing(true);
            }
        });
        new LoadDetailsTask().execute();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                getActivity().onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private class LoadDetailsTask extends AsyncTask<Void, Void, Void> {
        String error;

        @Override
        protected Void doInBackground(Void... params) {
            try {
                s.loadMangaInformation(m, true);
            } catch (Exception e) {
                if (e.getMessage() != null)
                    error = e.getMessage();
                else
                    error = e.getLocalizedMessage();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void v) {
            String infoExtra = "";
            if (error == null || error.length() < 2) {
                if (m.isFinished()) {
                    infoExtra = infoExtra + getResources().getString(R.string.finalizado);
                } else {
                    infoExtra = infoExtra + getResources().getString(R.string.en_progreso);
                }
                data.setStatus(infoExtra);
                data.setServer(s.getServerName());
                if (m.getAuthor() != null && m.getAuthor().length() > 1) {
                    data.setAuthor(m.getAuthor());
                } else {
                    data.setAuthor(getResources().getString(R.string.nodisponible));
                }
                if (m.getGenre() != null && m.getGenre().length() > 1) {
                    data.setGenre(m.getGenre());
                } else {
                    data.setGenre(getResources().getString(R.string.nodisponible));
                }
                if (m.getSynopsis() != null && m.getSynopsis().length() > 1) {
                    data.setSynopsis(m.getSynopsis());
                } else {
                    data.setSynopsis(getResources().getString(R.string.nodisponible));
                }
                imageLoader.displayImg(m.getImages(), data);
                if (error != null && error.length() > 2) {
                    Toast.makeText(getActivity(), error, Toast.LENGTH_LONG).show();
                } else {
                    AnimatorSet set = new AnimatorSet();
                    ObjectAnimator anim1 = ObjectAnimator.ofFloat(button_add, "alpha", 0.0f, 1.0f);
                    anim1.setDuration(0);
                    float y = button_add.getY();
                    DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
                    ObjectAnimator anim2 = ObjectAnimator.ofFloat(button_add, "y", displayMetrics.heightPixels);
                    anim2.setDuration(0);
                    ObjectAnimator anim3 = ObjectAnimator.ofFloat(button_add, "y", y);
                    anim3.setInterpolator(new AccelerateDecelerateInterpolator());
                    anim3.setDuration(500);
                    set.playSequentially(anim2, anim1, anim3);
                    set.start();
                }
            } else {
                Toast.makeText(getActivity(), error, Toast.LENGTH_LONG).show();
            }
            str.setRefreshing(false);
        }
    }

    public class AddMangaTask extends AsyncTask<Manga, Integer, Void> {
        ProgressDialog adding = new ProgressDialog(getActivity());
        String error = ".";
        int total = 0;

        @Override
        protected void onPreExecute() {
            adding.setMessage(getResources().getString(R.string.agregando));
            adding.show();
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Manga... params) {
            try {
                s.loadChapters(m, false);
            } catch (Exception e) {
                error = e.getMessage();
                Log.e(TAG, "Chapter load error", e);
            }
            total = params[0].getChapters().size();
            int mid = Database.addManga(getActivity(), params[0]);
            long initTime = System.currentTimeMillis();
            for (int i = 0; i < params[0].getChapters().size(); i++) {
                if (System.currentTimeMillis() - initTime > 500) {
                    publishProgress(i);
                    initTime = System.currentTimeMillis();
                }
                Database.addChapter(getActivity(), params[0].getChapter(i), mid);
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(final Integer... values) {
            super.onProgressUpdate(values);
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (adding != null) {
                        adding.setMessage(getResources().getString(R.string.agregando) + " " + values[0] + "/" + total);
                    }
                }
            });
        }

        @Override
        protected void onPostExecute(Void result) {
            adding.dismiss();
            Toast.makeText(getActivity(), getResources().getString(R.string.agregado), Toast.LENGTH_SHORT).show();
            if (error != null && error.length() > 2) {
                Toast.makeText(getActivity(), error, Toast.LENGTH_LONG).show();
            }
            getActivity().onBackPressed();
            super.onPostExecute(result);
        }
    }
}