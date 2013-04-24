/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package net.rptools.asset;

import java.util.concurrent.TimeoutException;

import net.rptools.asset.intern.Asset;

/**
 * Asynchrounous retrieval notification interface. To be implemented by callers of
 * getAssetAsync. Partial retrieval is notified at the asset handlers option.
 * Completion is also at the handler's option. E.g. continuous streams strictly
 * speaking never complete.
 * @author username
 */
public interface AssetListener {
    /**
     * Call-back method for completion. Please see the {@link #notifyPartial}
     * for semantics of <tt>obj</tt> in error cases.
     * @param id object id to inform about
     * @param obj completed object
     */
    public void notify(String id, Asset obj);

    /**
     * Call-back method for partial completion. This method can be called at
     * arbitrary times, generally not more often than 1/sec but not less than
     * 1/10sec. (Use common sense.) If completed == 1, this method need not be
     * called, but {@link #notify} will be used. This method need not
     * be called monotone fashion and is always a guess. If the request shall
     * be aborted, throw a TimeoutException. In that case notify is called with
     * a null. Note that this may effect other listeners of the same asset as
     * well.
     * @param id object id to inform about.
     * @param completed completion 0..1
     * @throws TimeoutException if the retrieval should be aborted. (Notify is
     *   not called. If a synchronous wait on the asset is pending, the result
     *   is undefined. It may be null, may contain a null object or may be
     *   complete.)
     */
    public void notifyPartial(String id, double completed) throws TimeoutException;
}
