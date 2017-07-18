/*
 * Copyright (c) 2016 Razeware LLC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.raywenderlich.cheesefinder;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;

import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Cancellable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;
import io.reactivex.schedulers.Schedulers;

import static io.reactivex.Observable.create;

public class CheeseActivity extends BaseSearchActivity {

    private Disposable mDisposable;

   //1 example for Button observable...
    private Observable<String> createButtonClickObservable() {
        return create(new ObservableOnSubscribe<String>() {

            @Override
            public void subscribe(final ObservableEmitter<String> e) throws Exception {
                mSearchButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        e.onNext(mQueryEditText.getText().toString());
                    }
                });

                e.setCancellable(new Cancellable() {
                    @Override
                    public void cancel() throws Exception {
                        mSearchButton.setOnClickListener(null);
                    }
                });
            }
        });
    }

    //2 example for TextView observale, when user type in textview it search and show result.
     private Observable<String> createTextChangeObservable(){
         Observable textObservable = Observable.create(new ObservableOnSubscribe<String>() {
             @Override
             public void subscribe(final ObservableEmitter<String> e) throws Exception {

                 final TextWatcher watcher= new TextWatcher() {
                     @Override
                     public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                     }

                     @Override
                     public void onTextChanged(CharSequence s, int start, int before, int count) {
                         e.onNext(s.toString());
                     }

                     @Override
                     public void afterTextChanged(Editable s) {
                     }
                 };

                 //add watcher into addTextChangedListener
                 mQueryEditText.addTextChangedListener(watcher);

                 e.setCancellable(new Cancellable() {
                     @Override
                     public void cancel() throws Exception {
                         //remove watcher from removeTextChangedListener
                         mQueryEditText.removeTextChangedListener(watcher);
                     }
                 });
             }

         });

//        filter operator's Predicate returns data when two digit typed by user.
//        debounce operator waits for 1000 milliseconds before emitting the latest query text.
         return textObservable.filter(new Predicate<String>() {
             @Override
             public boolean test(String query) throws Exception {
                 return query.length() >= 2;
             }
         }).debounce(1000, TimeUnit.MILLISECONDS);
     }

    @Override
    protected void onStart() {
        super.onStart();
        ObservableStart();

    }

    private void ObservableStart(){
// 1 for ButtonObservable
//    Observable<String> searchTextObservable = createButtonObservable();
// 2 for textObservable
//    Observable<String> searchTextObservable = createTextObservable();

        Observable<String> buttonClickStream = createButtonClickObservable();
        Observable<String> textChangeStream = createTextChangeObservable();

        Observable<String> searchTextObservable = Observable.merge(textChangeStream, buttonClickStream);

        // add Observable object into disposable
        mDisposable= searchTextObservable
                // 2 show progressbar through mainThread
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext(new Consumer<String>() {
                    @Override
                    public void accept(String s) throws Exception {
                        showProgressBar();
                    }
                })

                //3 run method under the Schedulers.io thread
                .observeOn(Schedulers.io())
                .map(new Function<String, List<String>>() {
                    @Override
                    public List<String> apply(String s) throws Exception {
                        return mCheeseSearchEngine.search(s);
                    }
                })

                // 4 show data under the main thread
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<List<String>>() {
                    @Override
                    public void accept(List<String> strings) throws Exception {
                        hideProgressBar();
                        showResult(strings);
                    }
                });
    }

    // dispose Disposable object
    @Override
    protected void onStop() {
        super.onStop();
        if (!mDisposable.isDisposed()) {
            mDisposable.dispose();
        }
    }
}
