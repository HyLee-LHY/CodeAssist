package com.tyron.code.ui.editor;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.tyron.builder.log.LogViewModel;
import com.tyron.code.R;
import com.tyron.code.ui.editor.log.AppLogFragment;
import com.tyron.code.ui.editor.shortcuts.ShortcutItem;
import com.tyron.code.ui.editor.shortcuts.ShortcutsAdapter;
import com.tyron.code.ui.editor.shortcuts.action.CursorMoveAction;
import com.tyron.code.ui.editor.shortcuts.action.RedoAction;
import com.tyron.code.ui.editor.shortcuts.action.TextInsertAction;
import com.tyron.code.ui.editor.shortcuts.action.UndoAction;
import com.tyron.code.ui.main.MainViewModel;
import com.tyron.code.util.AndroidUtilities;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@SuppressWarnings("FieldCanBeLocal")
public class BottomEditorFragment extends Fragment {

    public static final String OFFSET_KEY = "offsetKey";

    public static BottomEditorFragment newInstance() {
        return new BottomEditorFragment();
    }

    private View mRoot;

    private TabLayout mTabLayout;
    private LinearLayout mRowLayout;
    private ViewPager2 mPager;
    private PageAdapter mAdapter;

    private RecyclerView mShortcutsRecyclerView;

    private MainViewModel mFilesViewModel;

    public BottomEditorFragment() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mRoot = inflater.inflate(R.layout.bottom_editor_fragment, container, false);
        mRowLayout = mRoot.findViewById(R.id.row_layout);
        mShortcutsRecyclerView = mRoot.findViewById(R.id.recyclerview_shortcuts);
        mPager = mRoot.findViewById(R.id.viewpager);
        mTabLayout = mRoot.findViewById(R.id.tablayout);

        mFilesViewModel = new ViewModelProvider(requireActivity())
                .get(MainViewModel.class);
        return mRoot;
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAdapter = new PageAdapter(this);
        mPager.setAdapter(mAdapter);
        mPager.setUserInputEnabled(false);
        new TabLayoutMediator(mTabLayout, mPager, (tab, position) -> {
            switch (position) {
                case 0:
                    tab.setText("Build Logs");
                    break;
                default:
                case 1:
                    tab.setText("App Logs");
                    break;
                case 2:
                    tab.setText("Debug");
            }
        }).attach();

        ShortcutsAdapter adapter = new ShortcutsAdapter(getShortcuts());
        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext(),
                LinearLayoutManager.HORIZONTAL, false);
        mShortcutsRecyclerView.setLayoutManager(layoutManager);
        mShortcutsRecyclerView.setAdapter(adapter);

        adapter.setOnShortcutSelectedListener((item, pos) -> {
            File currentFile = mFilesViewModel.getCurrentFile();
            if (currentFile != null) {
                CodeEditorFragment fragment = (CodeEditorFragment) getParentFragmentManager()
                        .findFragmentByTag("f" + currentFile.getAbsolutePath().hashCode());
                if (fragment != null) {
                    fragment.performShortcut(item);
                }
            }
        });

        getParentFragmentManager().setFragmentResultListener(OFFSET_KEY, getViewLifecycleOwner(),
                ((requestKey, result) -> {
            setOffset(result.getFloat("offset", 0f));
        }));
    }

    private void setOffset(float offset) {
        if (mRowLayout == null) {
            return;
        }

        if (offset >= 0.50f) {
            float invertedOffset = 0.5f - offset;
            setRowOffset(((invertedOffset + 0.5f) * 2f));
        } else {
            if (mRowLayout.getHeight() != AndroidUtilities.dp(30)) {
                setRowOffset(1f);
            }
        }
    }

    private void setRowOffset(float offset) {
        mRowLayout.getLayoutParams()
                .height = Math.round(AndroidUtilities.dp(38) * offset);
        mRowLayout.requestLayout();
    }

    private List<ShortcutItem> getShortcuts() {
        List<String> strings = Arrays.asList("<", ">", ";", "{", "}", ":");
        List<ShortcutItem> items = strings.stream()
                .map(item -> {
                    ShortcutItem it = new ShortcutItem();
                    it.label = item;
                    it.kind = TextInsertAction.KIND;
                    it.actions = Collections.singletonList(new TextInsertAction());
                    return it;
                }).collect(Collectors.toList());
        Collections.addAll(items,
                new ShortcutItem(Collections.singletonList(new UndoAction()), "⬿", UndoAction.KIND),
                new ShortcutItem(Collections.singletonList(new RedoAction()), "⤳", RedoAction.KIND),
                new ShortcutItem(Collections.singletonList(new CursorMoveAction(CursorMoveAction.Direction.UP, 1)), "↑", CursorMoveAction.KIND),
                new ShortcutItem(Collections.singletonList(new CursorMoveAction(CursorMoveAction.Direction.DOWN, 1)), "↓", CursorMoveAction.KIND),
                new ShortcutItem(Collections.singletonList(new CursorMoveAction(CursorMoveAction.Direction.LEFT, 1)), "←", CursorMoveAction.KIND),
                new ShortcutItem(Collections.singletonList(new CursorMoveAction(CursorMoveAction.Direction.RIGHT, 1)), "→", CursorMoveAction.KIND)
        );

        return items;
    }

    @SuppressWarnings("deprecation")
    private static class PageAdapter extends FragmentStateAdapter {

        public PageAdapter(@NonNull Fragment fragment) {
            super(fragment);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case 0:
                    return AppLogFragment.newInstance(LogViewModel.BUILD_LOG);
                default:
                case 1:
                    return AppLogFragment.newInstance(LogViewModel.APP_LOG);
                case 2:
                    return AppLogFragment.newInstance(LogViewModel.DEBUG);
            }
        }

        @Override
        public int getItemCount() {
            return 3;
        }
    }
}
