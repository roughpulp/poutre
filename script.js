"use strict";

var cursors = Packages.com.roughpulp.commons.cursors.Cursors;
var inputs = Packages.com.roughpulp.poutre.pipeline.input.InputProcessors;
var senders = Packages.com.roughpulp.poutre.pipeline.senders.Senders;
var pipeline = Packages.com.roughpulp.poutre.pipeline.Pipeline;
var configReader = Packages.com.roughpulp.poutre.http_client.HttpClientConfig;

var request = {
    method: "POST",
    uri: "http://www.roughpulp.com/index.html",
    headers: {
        "User-Agent": "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:38.0) Gecko/20100101 Firefox/38.0",
        "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
        "Accept-Language": "en-US,en;q=0.5",
        "Accept-Encoding": "gzip, deflate",
        "Content-Type": "text/plain; charset=UTF-8"
    },
    entity: 'yadiyada'
};

var config = {
	soTimeout: 10 * 1000,
	soLinger: 10 * 1000,
	tcpNoDelay: true,
	connReuse: false,
	proxy: null,
    threads: 16,
	liveStatsPeriodSec: 2,
	dryRun: false,
	errors: "errors.log.gz"
};

var processors = [
    inputs.constant(request),
    inputs.fromMapToHttpRequest(),
    inputs.timeLimitSec(30),
    inputs.rateLimit(1.0 / 5.0),
    senders.syncVariableThreads(config)
];

pipeline.run(processors);