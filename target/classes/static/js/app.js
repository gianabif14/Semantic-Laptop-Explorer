document.addEventListener("DOMContentLoaded", () => {
    const $ = (id) => document.getElementById(id);
    const brandSelect = $("brandSelect");
    const categorySelect = $("categorySelect");
    const ramSelect = $("ramSelect");
    const priceSelect = $("priceSelect");
    const sortSelect = $("sortSelect");
    const keywordInput = $("keywordInput");
    const filterForm = $("filterForm");
    const resetBtn = $("resetBtn");
    const alertBox = $("alertBox");
    const resultCount = $("resultCountBadge");
    const totalCount = $("totalCount");
    const listEl = $("laptopList");
    const detailEmpty = $("detailEmpty");
    const detailView = $("detailView");
    const heroBadges = $("heroBadges");
    const heroTitle = $("heroTitle");
    const heroDesc = $("heroDesc");
    const heroImage = $("heroImage");
    const googleImageLink = $("googleImageLink");
    const detailPrice = $("detailPrice");
    const detailPanel = $("detailPanel");
    const tabContent = $("tabContent");
    const tabButtons = document.querySelectorAll(".tab-btn");
    const synChips = document.querySelectorAll(".syn-chip");
    const closeDetail = $("closeDetail");
    function hideDetail() { detailPanel.classList.add("hidden"); document.body.classList.remove("overflow-hidden"); }
    closeDetail.addEventListener("click", hideDetail);
    detailPanel.addEventListener("click", (event) => { if (event.target === detailPanel) hideDetail(); });
    document.addEventListener("keydown", (event) => { if (event.key === "Escape") hideDetail(); });


    const idr = new Intl.NumberFormat("id-ID");
    const NS = "http://example.org/laptop-ontology#";

    const STOPWORDS = new Set([
        "untuk", "buat", "bagi", "yang", "dengan", "dan", "atau", "di", "ke", "dari",
        "pada", "dalam", "adalah", "ini", "itu", "cari", "carikan", "rekomendasi",
        "tolong", "tampilkan", "pilihkan", "saya", "aku", "mau", "ingin", "butuh",
        "perlu", "cocok", "bagus", "terbaik", "paling", "harga", "spesifikasi", "spek",
        "tipe", "seri", "model", "kategori", "kelas", "laptop", "notebook", "komputer",
        "perangkat", "pc", "pakai", "for", "with", "the", "a", "an", "in", "on", "of", "to", "and", "or"
    ]);

    let allLaptops = [];      // hasil API (sebelum filter kategori)
    let shown = [];           // hasil akhir yang ditampilkan
    let selectedModel = null;
    let activeTab = "ringkasan";
    let categoriesLoaded = false;
    let totalLoaded = false;
    let debounceTimer = null;
    let requestSeq = 0;

    /* ---------- util ---------- */
    function el(tag, className, text) {
        const e = document.createElement(tag);
        if (className) e.className = className;
        if (text !== undefined) e.textContent = text;
        return e;
    }

    function escapeHtml(s) {
        return String(s).replace(/[&<>"']/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));
    }

    function formatStorage(l) {
        const gb = l.storageGb;
        const size = gb >= 1024 && gb % 1024 === 0 ? `${gb / 1024} TB` : `${gb} GB`;
        return `${size} ${l.storageType}`.trim();
    }

    // Foto produk resmi eksternal (CDN produsen); tidak disimpan di dalam ZIP.
    // Model yang belum memiliki foto terverifikasi menampilkan placeholder spesifik + tautan Google Images.
    const PRODUCT_IMAGES = [
        { match: /chromebook cx1/i, url: "https://dlcdnwebimgs.asus.com/gain/64361e88-38ba-48d5-8367-502468cc5855/w800" },
        { match: /vivobook go 14 e1404/i, url: "https://press.asus.com/assets/w_640%2Ch_640/1e0abc93-461e-41d2-bb05-4f9e04ace884/Vivobook%20Go%2014_E1404F_Thin%20and%20light.png" },
        { match: /vivobook 14 x1404/i, url: "https://dlcdnwebimgs.asus.com/gain/8e05f618-e30b-4f59-8970-09d828d23b9d/w800" },
        { match: /vivobook 15 x1504/i, url: "https://dlcdnwebimgs.asus.com/gain/d76a41bc-780e-41ef-938b-4ee8ac4a5634/w800" },
        { match: /expertbook b1/i, url: "https://dlcdnwebimgs.asus.com/gain/695663ab-72d8-4bc0-9eaf-e971c33872d2/w692" }
    ];

    function productImageUrl(l) {
        const item = PRODUCT_IMAGES.find((entry) => entry.match.test(`${l.brand} ${l.model}`));
        return item ? item.url : "";
    }

    function googleImageSearchUrl(l) {
        return `https://www.google.com/search?tbm=isch&q=${encodeURIComponent(`${l.brand} ${l.model} laptop official product photo`)}`;
    }

    function imagePlaceholder(l, className = "") {
        const box = el("div", `product-image-placeholder ${className}`);
        box.appendChild(el("div", "text-4xl mb-2", "▱"));
        box.appendChild(el("div", "text-sm font-bold text-slate-700", l.model));
        box.appendChild(el("div", "text-xs text-slate-500 mt-1", "Foto resmi belum dipetakan"));
        box.appendChild(el("div", "text-[11px] text-slate-500 mt-2", "Buka detail untuk tautan foto model"));
        return box;
    }

    function initials(brand) {
        return (brand || "?").split(/\s+/).map((w) => w[0]).join("").slice(0, 2).toUpperCase();
    }

    function splitSynonyms(str) {
        return (str || "").split(",").map((s) => s.trim()).filter(Boolean);
    }

    function matchedSynonym(l, kw) {
        const kwLower = kw.toLowerCase();
        if (!kwLower) return null;
        const items = splitSynonyms(l.synonyms);
        for (const it of items) if (it.toLowerCase().includes(kwLower)) return it;
        const raw = kwLower.split(/[^a-z0-9.+\-]+/).filter(Boolean);
        const tokens = raw.filter((t) => !STOPWORDS.has(t));
        for (const t of (tokens.length ? tokens : raw)) {
            for (const it of items) if (it.toLowerCase().includes(t)) return it;
        }
        return null;
    }

    function showError(msg) { alertBox.textContent = msg; alertBox.classList.remove("hidden"); }
    function hideError() { alertBox.textContent = ""; alertBox.classList.add("hidden"); }

    /* ---------- data ---------- */
    async function loadBrands() {
        try {
            const res = await fetch("/api/brands");
            if (!res.ok) throw new Error("Gagal memuat daftar brand");
            const brands = await res.json();
            brandSelect.innerHTML = '<option value="">Semua brand</option>';
            brands.forEach((b) => {
                const o = document.createElement("option");
                o.value = b;
                o.textContent = b;
                brandSelect.appendChild(o);
            });
        } catch (err) {
            showError("Tidak dapat memuat daftar brand: " + err.message);
        }
    }

    async function runSearch() {
        hideError();
        const kw = keywordInput.value.trim();
        syncSynChip(kw);
        const params = new URLSearchParams({
            brand: brandSelect.value,
            minRam: ramSelect.value,
            maxPrice: priceSelect.value,
            keyword: kw
        });
        const seq = ++requestSeq;
        try {
            const res = await fetch(`/api/laptops?${params.toString()}`);
            const payload = await res.json();
            if (seq !== requestSeq) return; // respons usang
            if (!res.ok) throw new Error(payload.error || "Query gagal dijalankan.");
            allLaptops = Array.isArray(payload) ? payload : [];

            if (!categoriesLoaded) {
                const cats = [...new Set(allLaptops.map((l) => l.category).filter(Boolean))].sort((a, b) => a.localeCompare(b, "id"));
                cats.forEach((c) => {
                    const o = document.createElement("option");
                    o.value = c;
                    o.textContent = c;
                    categorySelect.appendChild(o);
                });
                categoriesLoaded = true;
            }
            if (!totalLoaded && !kw && !brandSelect.value && ramSelect.value === "0" && priceSelect.value === "100000000") {
                totalCount.textContent = String(allLaptops.length);
                totalLoaded = true;
            }
            applyClientView(kw);
        } catch (err) {
            if (seq === requestSeq) showError(err.message);
        }
    }

    function applyClientView(kw) {
        const cat = categorySelect.value;
        const [field, order] = sortSelect.value.split(":");
        shown = allLaptops.filter((l) => !cat || l.category === cat);

        // Jika sorting default (harga naik) dan ada keyword, urutan relevansi dari server dipertahankan
        shown.sort((a, b) => {
            const va = a[field], vb = b[field];
            const cmp = (typeof va === "number" && typeof vb === "number")
                ? va - vb
                : String(va).localeCompare(String(vb), "id", { sensitivity: "base" });
            return order === "asc" ? cmp : -cmp;
        });

        resultCount.textContent = `${shown.length} hasil ditemukan`;
        document.title = `Semantic Laptop Explorer — ${shown.length} hasil`;

        if (!shown.some((l) => l.model === selectedModel)) {
            selectedModel = shown.length ? shown[0].model : null;
        }
        renderList(kw);
        renderDetail();
    }

    /* ---------- list (master) ---------- */
    function renderList(kw) {
        listEl.innerHTML = "";
        if (!shown.length) {
            listEl.appendChild(el("div", "col-span-full bg-white rounded-2xl border border-blue-100 p-10 text-center text-sm text-slate-500", "Tidak ada laptop yang memenuhi kriteria filter atau sinonim yang dicari."));
            return;
        }
        shown.forEach((l) => {
            const card = el("button", "laptop-card w-full flex flex-col"); card.type = "button";
            card.setAttribute("aria-label", `Lihat detail ${l.model}`);
            const imageWrap = el("div", "relative h-48 bg-gradient-to-br from-[#edf3fb] to-[#f7f0df] p-4 flex items-center justify-center");
            const imageUrl = productImageUrl(l);
            if (imageUrl) {
                const img = document.createElement("img");
                img.src = imageUrl;
                img.alt = `Foto produk ${l.brand} ${l.model}`;
                img.loading = "lazy";
                img.referrerPolicy = "no-referrer";
                img.className = "w-full h-full object-contain";
                img.onerror = () => { img.replaceWith(imagePlaceholder(l, "w-full h-full")); };
                imageWrap.appendChild(img);
            } else {
                imageWrap.appendChild(imagePlaceholder(l, "w-full h-full"));
            }
            imageWrap.appendChild(el("span", "absolute top-3 left-3 rounded-full bg-white/90 text-blue-900 text-xs font-bold px-3 py-1 shadow-sm", l.brand));
            card.appendChild(imageWrap);
            const body = el("div", "p-5 flex flex-col flex-1");
            body.appendChild(el("div", "text-xs uppercase tracking-wider text-blue-700 font-bold", l.category || "Laptop"));
            body.appendChild(el("h3", "font-extrabold text-slate-900 text-base leading-snug mt-1", l.model));
            body.appendChild(el("p", "text-xs text-slate-500 mt-1", l.processor));
            const specs = el("div", "grid grid-cols-2 gap-2 mt-4");
            [[`${l.ramGb} GB`, "RAM"], [formatStorage(l), "Storage"], [`${l.screenInch} inch`, "Layar"], [l.gpu, "GPU"]].forEach(([v,k]) => {
                const item=el("div","rounded-xl bg-[#f4f6fa] px-3 py-2 min-w-0"); item.appendChild(el("div","text-xs font-bold text-slate-800 truncate",v)); item.appendChild(el("div","text-[10px] text-slate-500",k)); specs.appendChild(item);
            });
            body.appendChild(specs);
            const foot=el("div","mt-auto pt-4 flex items-center justify-between gap-2"); foot.appendChild(el("span","text-xs text-blue-700 font-semibold","Lihat detail →")); foot.appendChild(el("span","text-sm font-extrabold text-[#173b64]",`Rp ${idr.format(l.priceIdr)}`)); body.appendChild(foot);
            card.appendChild(body);
            card.addEventListener("click",()=>{selectedModel=l.model;activeTab="ringkasan";renderDetail();detailPanel.classList.remove("hidden");document.body.classList.add("overflow-hidden");});
            listEl.appendChild(card);
        });
    }

    /* ---------- detail ---------- */
    function currentLaptop() {
        return shown.find((l) => l.model === selectedModel) || null;
    }

    function renderDetail() {
        const l = currentLaptop();
        if (!l) {
            detailView.classList.add("hidden");
            detailEmpty.classList.remove("hidden");
            return;
        }
        detailEmpty.classList.add("hidden");
        detailView.classList.remove("hidden");

        heroBadges.innerHTML = "";
        heroBadges.appendChild(el("span", "text-xs font-bold px-3 py-1 rounded-full bg-cream-200 text-navy-900", l.brand));
        if (l.category) heroBadges.appendChild(el("span", "text-xs font-bold px-3 py-1 rounded-full bg-white/15 border border-white/30", l.category));
        heroTitle.textContent = l.model;
        heroDesc.textContent = `${l.processor} · ${l.ramGb} GB RAM · ${formatStorage(l)} · ${l.gpu} · layar ${l.screenInch}" · ${l.operatingSystem}`;
        const detailImageUrl = productImageUrl(l);
        const oldPlaceholder = heroImage.parentElement.querySelector(".product-image-placeholder");
        if (oldPlaceholder) oldPlaceholder.remove();
        if (detailImageUrl) {
            heroImage.src = detailImageUrl;
            heroImage.alt = `Foto produk ${l.brand} ${l.model}`;
            heroImage.classList.remove("hidden");
            heroImage.onerror = () => {
                heroImage.classList.add("hidden");
                heroImage.parentElement.appendChild(imagePlaceholder(l, "w-full min-h-48"));
            };
        } else {
            heroImage.classList.add("hidden");
            heroImage.parentElement.appendChild(imagePlaceholder(l, "w-full min-h-48"));
        }
        googleImageLink.href = googleImageSearchUrl(l);
        detailPrice.textContent = `Rp ${idr.format(l.priceIdr)}`;

        tabButtons.forEach((b) => {
            const on = b.dataset.tab === activeTab;
            b.setAttribute("aria-selected", on ? "true" : "false");
            b.className = "tab-btn px-4 py-3 text-sm font-semibold border-b-2 transition " +
                (on ? "border-navy-700 text-navy-800" : "border-transparent text-navy-400 hover:text-navy-700");
        });

        tabContent.innerHTML = "";
        if (activeTab === "ringkasan") renderSummary(l);
        else if (activeTab === "spesifikasi") renderSpecs(l);
        else if (activeTab === "relasi") renderRelations(l);
        else renderTriples(l);
    }

    function kvGrid(pairs) {
        const grid = el("dl", "grid grid-cols-1 sm:grid-cols-2 gap-x-8 gap-y-4");
        pairs.forEach(([k, v]) => {
            const row = el("div", "border-b border-cream-300 pb-2");
            row.appendChild(el("dt", "text-xs uppercase tracking-wide text-navy-400 font-semibold", k));
            row.appendChild(el("dd", "text-sm font-semibold text-navy-900 mt-0.5", v || "—"));
            grid.appendChild(row);
        });
        return grid;
    }

    function specPairs(l) {
        return [
            ["Brand / Merek", l.brand],
            ["Processor", l.processor],
            ["RAM", `${l.ramGb} GB`],
            ["Storage", formatStorage(l)],
            ["Ukuran layar", `${l.screenInch} inch`],
            ["GPU", l.gpu],
            ["Sistem operasi", l.operatingSystem],
            ["Rentang harga", l.category]
        ];
    }

    function renderSummary(l) {
        const wrap = el("div", "grid grid-cols-1 md:grid-cols-3 gap-6");
        const left = el("div", "md:col-span-2");
        left.appendChild(kvGrid(specPairs(l).slice(0, 6)));
        wrap.appendChild(left);

        const box = el("div", "rounded-2xl bg-cream-100 border border-cream-300 p-5 self-start");
        box.appendChild(el("div", "text-xs uppercase tracking-wide text-navy-400 font-semibold", "Harga"));
        box.appendChild(el("div", "text-2xl font-extrabold text-navy-800 mt-1", `Rp ${idr.format(l.priceIdr)}`));
        if (l.category) box.appendChild(el("div", "mt-3 text-xs text-navy-600", `Kategori: ${l.category}`));
        box.appendChild(el("div", "mt-3 text-xs text-navy-600", `${l.ramGb} GB RAM · ${formatStorage(l)}`));
        wrap.appendChild(box);
        tabContent.appendChild(wrap);
    }

    function renderSpecs(l) {
        tabContent.appendChild(kvGrid(specPairs(l).concat([["Harga", `Rp ${idr.format(l.priceIdr)}`]])));
    }

    function renderRelations(l) {
        const rels = [
            ["diproduksiOleh", "Brand", l.brand],
            ["memilikiProcessor", "Processor", l.processor],
            ["memilikiRAM", "RAM", `${l.ramGb} GB`],
            ["memilikiStorage", "Storage", formatStorage(l)],
            ["memilikiGPU", "GPU", l.gpu],
            ["memilikiOperatingSystem", "Sistem operasi", l.operatingSystem]
        ];
        if (l.category) rels.push(["memilikiPriceRange", "Rentang harga", l.category]);

        const wrap = el("div", "grid grid-cols-1 md:grid-cols-2 gap-6");

        const relBox = el("div");
        relBox.appendChild(el("h3", "text-sm font-bold text-navy-800 mb-3", "Relasi objek (object property)"));
        const ul = el("ul", "space-y-2");
        rels.forEach(([p, label, val]) => {
            const li = el("li", "flex items-center gap-2 text-sm bg-cream-50 border border-cream-300 rounded-xl px-3 py-2");
            li.appendChild(el("code", "text-xs text-teal-700", `:${p}`));
            li.appendChild(el("span", "text-navy-300", "→"));
            li.appendChild(el("span", "font-semibold text-navy-900", `${label}: ${val}`));
            ul.appendChild(li);
        });
        relBox.appendChild(ul);
        wrap.appendChild(relBox);

        const synBox = el("div", "rounded-2xl bg-cream-100 border border-cream-300 p-5 self-start");
        synBox.appendChild(el("h3", "text-sm font-bold text-navy-800 mb-1", "Sinonim semantik (SKOS)"));
        synBox.appendChild(el("p", "text-xs text-navy-500 mb-3", "Kata yang dikenali pencarian untuk model ini."));
        const syns = splitSynonyms(l.synonyms);
        if (syns.length) {
            const chips = el("div", "flex flex-wrap gap-2");
            const kw = keywordInput.value.trim();
            const hit = matchedSynonym(l, kw);
            syns.forEach((s) => {
                const isHit = hit && s === hit;
                chips.appendChild(el("span",
                    "text-xs px-2.5 py-1 rounded-full border " + (isHit ? "bg-emerald-100 border-emerald-300 text-emerald-800 font-semibold" : "bg-white border-navy-200 text-navy-700"), s));
            });
            synBox.appendChild(chips);
        } else {
            synBox.appendChild(el("p", "text-xs text-navy-400", "Tidak ada sinonim terdaftar."));
        }
        wrap.appendChild(synBox);
        tabContent.appendChild(wrap);
    }

    /* ---------- Triple RDF ---------- */
    function slug(s) {
        return String(s).replace(/[^A-Za-z0-9]+/g, "_").replace(/^_+|_+$/g, "");
    }

    function buildTriples(l) {
        const subj = `:${slug(l.model)}`;
        const T = (s, p, o) => ({ s, p, o });
        const lit = (v) => `"${String(v).replace(/\\/g, "\\\\").replace(/"/g, '\\"')}"`;
        const num = (v, type) => `"${v}"^^xsd:${type}`;
        const t = [
            T(subj, "rdf:type", ":Laptop"),
            T(subj, ":namaModel", lit(l.model)),
            T(subj, ":diproduksiOleh", `:${slug(l.brand)}`),
            T(`:${slug(l.brand)}`, ":namaBrand", lit(l.brand)),
            T(subj, ":memilikiProcessor", `:${slug(l.processor)}`),
            T(`:${slug(l.processor)}`, ":namaProcessor", lit(l.processor)),
            T(subj, ":memilikiRAM", `:RAM_${l.ramGb}GB`),
            T(`:RAM_${l.ramGb}GB`, ":kapasitasGB", num(l.ramGb, "integer")),
            T(subj, ":memilikiStorage", `:Storage_${l.storageGb}GB_${slug(l.storageType)}`),
            T(`:Storage_${l.storageGb}GB_${slug(l.storageType)}`, ":kapasitasStorageGB", num(l.storageGb, "integer")),
            T(`:Storage_${l.storageGb}GB_${slug(l.storageType)}`, ":jenisStorage", lit(l.storageType)),
            T(subj, ":memilikiDisplay", `:Display_${String(l.screenInch).replace(".", "_")}in`),
            T(`:Display_${String(l.screenInch).replace(".", "_")}in`, ":ukuranLayarInch", num(l.screenInch, "decimal")),
            T(subj, ":memilikiGPU", `:${slug(l.gpu)}`),
            T(`:${slug(l.gpu)}`, ":namaGPU", lit(l.gpu)),
            T(subj, ":memilikiOperatingSystem", `:${slug(l.operatingSystem)}`),
            T(`:${slug(l.operatingSystem)}`, ":namaOS", lit(l.operatingSystem)),
            T(subj, ":harga", num(l.priceIdr, "integer"))
        ];
        if (l.category) {
            t.push(T(subj, ":memilikiPriceRange", `:${slug(l.category)}`));
            t.push(T(`:${slug(l.category)}`, ":namaRange", lit(l.category)));
        }
        return t;
    }

    function renderTriples(l) {
        const triples = buildTriples(l);
        const head = el("div", "flex items-center justify-between mb-3 gap-3 flex-wrap");
        head.appendChild(el("p", "text-xs text-navy-500",
            `${triples.length} triple (Turtle) disusun dari hasil query SPARQL untuk model ini. Nama individu diturunkan dari label.`));
        const copyBtn = el("button", "text-xs font-semibold px-3 py-1.5 rounded-lg bg-navy-700 hover:bg-navy-800 text-cream-100 transition", "Salin Turtle");
        copyBtn.type = "button";
        head.appendChild(copyBtn);
        tabContent.appendChild(head);

        const plain = [
            `@prefix : <${NS}> .`,
            "@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .",
            "@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .",
            "",
            ...triples.map((x) => `${x.s} ${x.p} ${x.o} .`)
        ].join("\n");

        const html = [
            `<span class="tok-prefix">@prefix</span> : &lt;${escapeHtml(NS)}&gt; .`,
            `<span class="tok-prefix">@prefix</span> rdf: &lt;http://www.w3.org/1999/02/22-rdf-syntax-ns#&gt; .`,
            `<span class="tok-prefix">@prefix</span> xsd: &lt;http://www.w3.org/2001/XMLSchema#&gt; .`,
            "",
            ...triples.map((x) =>
                `<span class="tok-subj">${escapeHtml(x.s)}</span> <span class="tok-pred">${escapeHtml(x.p)}</span> ` +
                `<span class="${x.o.startsWith('"') ? "tok-lit" : "tok-subj"}">${escapeHtml(x.o)}</span> .`)
        ].join("\n");

        const pre = el("pre", "bg-cream-50 border border-cream-300 rounded-xl p-4 text-xs leading-relaxed overflow-x-auto scroll-thin font-mono max-h-96");
        pre.innerHTML = html;
        tabContent.appendChild(pre);

        copyBtn.addEventListener("click", async () => {
            try {
                await navigator.clipboard.writeText(plain);
                copyBtn.textContent = "Tersalin ✓";
            } catch {
                copyBtn.textContent = "Gagal menyalin";
            }
            setTimeout(() => { copyBtn.textContent = "Salin Turtle"; }, 1500);
        });
    }

    /* ---------- events ---------- */
    tabButtons.forEach((b) => b.addEventListener("click", () => {
        activeTab = b.dataset.tab;
        renderDetail();
    }));

    filterForm.addEventListener("submit", (e) => { e.preventDefault(); runSearch(); });
    brandSelect.addEventListener("change", runSearch);
    ramSelect.addEventListener("change", runSearch);
    priceSelect.addEventListener("change", runSearch);
    categorySelect.addEventListener("change", () => applyClientView(keywordInput.value.trim()));
    sortSelect.addEventListener("change", () => applyClientView(keywordInput.value.trim()));

    keywordInput.addEventListener("input", () => {
        clearTimeout(debounceTimer);
        debounceTimer = setTimeout(runSearch, 180);
    });

    synChips.forEach((chip) => chip.addEventListener("click", () => {
        const kw = chip.dataset.kw;
        keywordInput.value = keywordInput.value.trim().toLowerCase() === kw.toLowerCase() ? "" : kw;
        runSearch();
    }));

    function syncSynChip(kw) {
        const lower = kw.toLowerCase();
        synChips.forEach((chip) => {
            const on = chip.dataset.kw.toLowerCase() === lower;
            chip.classList.toggle("bg-navy-700", on);
            chip.classList.toggle("text-cream-100", on);
            chip.classList.toggle("bg-cream-50", !on);
        });
    }

    resetBtn.addEventListener("click", () => {
        brandSelect.value = "";
        categorySelect.value = "";
        ramSelect.value = "0";
        priceSelect.value = "100000000";
        sortSelect.value = "priceIdr:asc";
        keywordInput.value = "";
        activeTab = "ringkasan";
        runSearch();
    });

    loadBrands().then(runSearch);
});
