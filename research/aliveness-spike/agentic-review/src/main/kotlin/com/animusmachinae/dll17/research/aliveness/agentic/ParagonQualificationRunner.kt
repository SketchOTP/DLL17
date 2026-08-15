package com.animusmachinae.dll17.research.aliveness.agentic

/**
 * The D016-G entry points that actually contact the router.
 *
 * Two of them, and the split is deliberate. [ParagonShakeout] uses a synthetic
 * review that is not, and never was, a frozen qualification fixture; it exists so
 * that a schema or transport mistake is discovered without spending the single
 * permitted formal attempt. [ParagonFormalQualification] runs the frozen suite
 * once.
 *
 * Neither is wired into CI, because both require a credential and a private
 * endpoint, and because a run that samples a language model cannot be reproduced
 * byte for byte. What CI checks is the committed result.
 */

/** Shared construction of the plain-inference reviewer route. */
public object ParagonReviewerRoute {

    public const val ROUTE_ID: String = "ParagonPlainInferenceRouteV1"

    /**
     * The router's own hint header, already supported before D016-G.
     *
     * It names a route *class*, not a model. D016-G leaves downstream model
     * selection to the router, so nothing here pins one, and the header carries
     * no model identity for the project to accidentally depend on.
     */
    public const val FORCE_PROVIDER_HEADER: String = "x-paragon-force-provider"

    /** Read from the environment. The value never appears in evidence. */
    public fun apiKey(env: (String) -> String? = System::getenv): String =
        env(ParagonBackend.CREDENTIAL_ENV)
            ?: error("${ParagonBackend.CREDENTIAL_ENV} is not set; refusing to run")

    public fun route(env: (String) -> String? = System::getenv): String =
        env("PARAGON_ROUTE").takeUnless { it.isNullOrBlank() } ?: "openrouter"

    public fun backend(
        env: (String) -> String? = System::getenv,
        transport: HttpTransport = JdkHttpTransport(),
    ): ParagonBackend = ParagonBackend(
        modelId = "paragon",
        transport = transport,
        apiKey = apiKey(env),
        baseUrl = env("PARAGON_BASE_URL").takeUnless { it.isNullOrBlank() }
            ?: ParagonBackend.DEFAULT_BASE_URL,
        routeHeaders = listOf(FORCE_PROVIDER_HEADER to route(env)),
    )
}

/**
 * A non-scored transport and schema check.
 *
 * The review below is invented for this purpose. It is not a `MetaEvaluationSuite`
 * fixture, it is not scored, and its result is not evidence for anything: it only
 * answers "can this route return the four required fields at all".
 */
public object ParagonShakeout {

    @JvmStatic
    public fun main(args: Array<String>) {
        val backend = ParagonReviewerRoute.backend()
        val question = ReviewQuestion(
            "Q-SHAKEOUT",
            "Does the cited evidence support the stated conclusion?",
        )
        val bundle = EvidenceBundle(
            "B-SHAKEOUT",
            listOf(
                EvidenceItem(
                    "EV-01",
                    EvidenceKind.STATISTICAL_RESULT,
                    "A synthetic, non-scored example used only to check the response schema. " +
                        "It reports a mean difference of 12.0 with a 95% interval of [8.0, 16.0] " +
                        "against a threshold of 10.0; finding=PASS.",
                    true,
                ),
            ),
        )
        val outcome = IsolatedReviewSession.run(
            AgenticRoleContracts.PRIMARY, backend, question, bundle,
        )
        println("SHAKEOUT route=${ParagonReviewerRoute.route()}")
        println("SHAKEOUT outcome=${outcome.label()}")
        if (outcome is RulingOutcome.FailedClosed) {
            println("SHAKEOUT failureMode=${outcome.mode} detail=${outcome.detail}")
        }
        println("SHAKEOUT routing=${backend.lastRouting?.render() ?: PARAGON_ROUTING_UNOBSERVABLE}")
    }
}

/**
 * The formal run.
 *
 * Executes `AgenticReviewerQualificationThresholdsV1` exactly once against the
 * frozen fixtures, prints the result, and exits zero whether it qualified or not.
 * A non-zero exit on failure would invite a re-run, and D016-G forbids re-running
 * until it passes: the first completed result is the evidence.
 */
public object ParagonFormalQualification {

    @JvmStatic
    public fun main(args: Array<String>) {
        val route = ParagonReviewerRoute.route()
        // A fresh backend per call: no shared conversation state, and no object
        // through which one reviewer could observe another's traffic.
        var lastRouting: String = PARAGON_ROUTING_UNOBSERVABLE
        val make: (RoleContract) -> ReviewerBackend = {
            ParagonReviewerRoute.backend().also { b ->
                lastRouting = b.lastRouting?.render() ?: lastRouting
            }
        }
        val runner = RealReviewerQualification(
            primaryFactory = make,
            alternateFactory = make,
            routingLabel = { lastRouting },
        )
        val result = runner.run()
        println("PARAGON_ROUTE=$route")
        println(result.render())
    }
}
